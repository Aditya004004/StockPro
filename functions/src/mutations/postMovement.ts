import * as functions from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { initializeApp } from "firebase-admin/app";

initializeApp();
const db = getFirestore();

interface PostMovementPayload {
  orgId: string;
  plantId: string;
  itemId: string;
  type: string;
  deltaMilli: number;
  idempotencyKey: string;
  reference?: string;
  reasonCode?: string;
  note?: string;
  locationId?: string;
  fromLocationId?: string;
  toLocationId?: string;
  unitCostPaise?: number;
}

/**
 * postMovement — the core server-authoritative write function.
 *
 * Flow (architecture.md §6):
 * 1. Verify App Check + Auth + Custom Claims (orgId, plantRoles).
 * 2. Check idempotencyKey for deduplication.
 * 3. Execute runTransaction:
 *    - Read item → validate constraints
 *    - Append ledger/{entryId}
 *    - Update item.onHandMilli projection
 *    - Evaluate low-stock alert
 * 4. Return { entryId, balanceAfterMilli, stockLevel }
 */
export const postMovement = functions.onCall(
  { enforceAppCheck: true },
  async (request) => {
    // ── 1. Auth & Claims ──
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Authentication required.");
    }

    const claims = request.auth.token;
    const payload = request.data as PostMovementPayload;

    // Validate required fields
    if (
      typeof payload.orgId !== 'string' || payload.orgId.length === 0 ||
      typeof payload.plantId !== 'string' || payload.plantId.length === 0 ||
      typeof payload.itemId !== 'string' || payload.itemId.length === 0 ||
      typeof payload.type !== 'string' || !['RECEIPT', 'ISSUE', 'ADJUSTMENT', 'PRODUCTION', 'SCRAP', 'TRANSFER_IN', 'TRANSFER_OUT'].includes(payload.type) ||
      typeof payload.idempotencyKey !== 'string' || payload.idempotencyKey.length === 0 ||
      typeof payload.deltaMilli !== 'number' || !Number.isFinite(payload.deltaMilli)
    ) {
      throw new functions.HttpsError("invalid-argument", "Missing required fields.");
    }
    if (payload.deltaMilli === 0) {
      throw new functions.HttpsError("invalid-argument", "deltaMilli must be non-zero.");
    }

    // Check org membership
    if (claims.orgId !== payload.orgId) {
      throw new functions.HttpsError("permission-denied", "Not a member of this organization.");
    }

    // Check plant role
    const plantRoles = claims.plantRoles as Record<string, string> | undefined;
    if (!plantRoles || !(payload.plantId in plantRoles)) {
      throw new functions.HttpsError("permission-denied", "No role in this plant.");
    }

    // Check role-based permission for movement type
    const role = plantRoles[payload.plantId];
    if (!isMovementAllowed(role, payload.type)) {
      throw new functions.HttpsError(
        "permission-denied",
        `Role '${role}' cannot perform '${payload.type}' movements.`
      );
    }

    // Require reason code for ADJUSTMENT and SCRAP
    if ((payload.type === "ADJUSTMENT" || payload.type === "SCRAP") && !payload.reasonCode) {
      throw new functions.HttpsError("invalid-argument", "Reason code required for adjustments and scrap.");
    }

    const plantRef = db.collection("organizations").doc(payload.orgId)
      .collection("plants").doc(payload.plantId);
    const itemRef = plantRef.collection("items").doc(payload.itemId);
    const ledgerRef = plantRef.collection("ledger");

    // ── 2 & 3. Atomic Transaction with Transactional Idempotency Check ──
    const result = await db.runTransaction(async (txn) => {
      // Transactional idempotency check eliminates race condition on duplicate submissions
      const existingEntry = await txn.get(
        ledgerRef.where("idempotencyKey", "==", payload.idempotencyKey).limit(1)
      );

      if (!existingEntry.empty) {
        const doc = existingEntry.docs[0];
        const data = doc.data();
        return {
          entryId: doc.id,
          balanceAfterMilli: data.balanceAfterMilli,
          stockLevel: deriveStockLevel(data.balanceAfterMilli, 0), // approximation
          deduplicated: true,
        };
      }

      const itemDoc = await txn.get(itemRef);
      if (!itemDoc.exists) {
        throw new functions.HttpsError("not-found", "Item not found.");
      }

      const item = itemDoc.data()!;
      if (!item.isActive) {
        throw new functions.HttpsError("failed-precondition", "Item is deactivated.");
      }

      const currentOnHand: number = item.onHandMilli ?? 0;
      const reserved: number = item.reservedMilli ?? 0;
      const reorderLevel: number = item.reorderLevelMilli ?? 0;

      // Validate ISSUE: cannot issue more than available (onHand - reserved)
      if (payload.type === "ISSUE" || payload.type === "TRANSFER_OUT") {
        const available = currentOnHand - reserved;
        if (Math.abs(payload.deltaMilli) > available) {
          throw new functions.HttpsError(
            "failed-precondition",
            `Insufficient stock. Only ${available / 1000} available.`
          );
        }
      }

      const balanceAfter = currentOnHand + payload.deltaMilli;

      // Create ledger entry
      const entryRef = ledgerRef.doc();
      txn.set(entryRef, {
        itemId: payload.itemId,
        itemCode: item.code ?? "",
        itemName: item.name ?? "",
        type: payload.type,
        deltaMilli: payload.deltaMilli,
        balanceAfterMilli: balanceAfter,
        unitCostPaise: payload.unitCostPaise ?? item.unitCostPaise ?? 0,
        reference: payload.reference ?? null,
        reasonCode: payload.reasonCode ?? null,
        note: payload.note ?? null,
        fromLocationId: payload.fromLocationId ?? null,
        toLocationId: payload.toLocationId ?? null,
        actorUid: request.auth!.uid,
        actorName: request.auth!.token.name ?? "",
        idempotencyKey: payload.idempotencyKey,
        clientCreatedAt: FieldValue.serverTimestamp(),
        createdAt: FieldValue.serverTimestamp(),
      });

      // Update item projection
      txn.update(itemRef, {
        onHandMilli: balanceAfter,
        lastMovementAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });

      const stockLevel = deriveStockLevel(balanceAfter, reorderLevel);

      return {
        entryId: entryRef.id,
        balanceAfterMilli: balanceAfter,
        stockLevel,
      };
    });

    return result;
  }
);

/**
 * Permission matrix check (architecture.md §5.3).
 */
function isMovementAllowed(role: string, type: string): boolean {
  const permissions: Record<string, string[]> = {
    STORE_KEEPER: ["RECEIPT", "ISSUE", "TRANSFER_IN", "TRANSFER_OUT"],
    PRODUCTION_SUPERVISOR: ["ISSUE", "PRODUCTION", "SCRAP"],
    QA: ["SCRAP"],
    INVENTORY_MANAGER: ["RECEIPT", "ISSUE", "PRODUCTION", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT", "SCRAP"],
    PLANT_MANAGER: ["RECEIPT", "ISSUE", "PRODUCTION", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT", "SCRAP"],
    ORG_ADMIN: ["RECEIPT", "ISSUE", "PRODUCTION", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT", "SCRAP"],
  };

  const allowed = permissions[role];
  return allowed ? allowed.includes(type) : false;
}

/**
 * Derive stock level from balance and reorder threshold.
 */
function deriveStockLevel(balanceMilli: number, reorderLevelMilli: number): string {
  if (balanceMilli <= 0) return "OUT_OF_STOCK";
  if (balanceMilli <= reorderLevelMilli) return "LOW_STOCK";
  return "IN_STOCK";
}
