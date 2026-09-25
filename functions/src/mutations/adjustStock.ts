import * as functions from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { deriveStockLevel } from "../domain/stockMath";

const db = getFirestore();

interface AdjustStockPayload {
  orgId: string;
  plantId: string;
  itemId: string;
  countedOnHandMilli: number;
  reasonCode: string;
  note?: string;
  idempotencyKey: string;
}

const DEFAULT_HIGH_VARIANCE_THRESHOLD_PAISE = 1_000_000; // ₹10,000 default

export const adjustStock = functions.onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Authentication required.");
    }

    const payload = request.data as AdjustStockPayload;
    if (
      typeof payload.orgId !== 'string' || payload.orgId.length === 0 ||
      typeof payload.plantId !== 'string' || payload.plantId.length === 0 ||
      typeof payload.itemId !== 'string' || payload.itemId.length === 0 ||
      typeof payload.countedOnHandMilli !== 'number' || !Number.isFinite(payload.countedOnHandMilli) ||
      typeof payload.reasonCode !== 'string' || payload.reasonCode.length === 0 ||
      typeof payload.idempotencyKey !== 'string' || payload.idempotencyKey.length === 0
    ) {
      throw new functions.HttpsError("invalid-argument", "Missing required fields for stock adjustment.");
    }

    if (payload.countedOnHandMilli < 0) {
      throw new functions.HttpsError("invalid-argument", "Counted stock cannot be negative.");
    }

    const claims = request.auth.token;
    if (claims.orgId !== payload.orgId) {
      throw new functions.HttpsError("permission-denied", "Not a member of this organization.");
    }

    const plantRoles = claims.plantRoles as Record<string, string> | undefined;
    const role = plantRoles?.[payload.plantId];
    if (!role || !["INVENTORY_MANAGER", "PLANT_MANAGER", "ORG_ADMIN"].includes(role)) {
      throw new functions.HttpsError("permission-denied", "Only Inventory Managers, Plant Managers, and Org Admins can adjust stock.");
    }

    const plantRef = db.collection("organizations").doc(payload.orgId)
      .collection("plants").doc(payload.plantId);
    const itemRef = plantRef.collection("items").doc(payload.itemId);
    const ledgerRef = plantRef.collection("ledger");
    const notificationsRef = plantRef.collection("notifications");

    const result = await db.runTransaction(async (txn) => {
      // Transactional idempotency check
      const existing = await txn.get(ledgerRef.where("idempotencyKey", "==", payload.idempotencyKey).limit(1));
      if (!existing.empty) {
        const doc = existing.docs[0];
        const data = doc.data();
        return {
          entryId: doc.id,
          balanceAfterMilli: data.balanceAfterMilli,
          deduplicated: true,
        };
      }

      const itemDoc = await txn.get(itemRef);
      if (!itemDoc.exists) {
        throw new functions.HttpsError("not-found", "Item not found.");
      }

      const item = itemDoc.data()!;
      if (!item.isActive) {
        throw new functions.HttpsError("failed-precondition", "Cannot adjust a deactivated item.");
      }

      const currentOnHand = item.onHandMilli ?? 0;
      const deltaMilli = payload.countedOnHandMilli - currentOnHand;
      const unitCostPaise = item.unitCostPaise ?? 0;
      const variancePaise = Math.abs(Math.round((deltaMilli / 1000) * unitCostPaise));

      const entryRef = ledgerRef.doc();
      txn.set(entryRef, {
        itemId: payload.itemId,
        itemCode: item.code ?? "",
        itemName: item.name ?? "",
        type: "ADJUSTMENT",
        deltaMilli,
        balanceAfterMilli: payload.countedOnHandMilli,
        unitCostPaise,
        reference: null,
        reasonCode: payload.reasonCode,
        note: payload.note ?? null,
        actorUid: request.auth!.uid,
        actorName: request.auth!.token.name ?? "",
        idempotencyKey: payload.idempotencyKey,
        createdAt: FieldValue.serverTimestamp(),
      });

      txn.update(itemRef, {
        onHandMilli: payload.countedOnHandMilli,
        lastMovementAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
        updatedBy: request.auth!.uid,
      });

      const plantDoc = await txn.get(plantRef);
      const configuredThresholdPaise = (plantDoc.exists && typeof plantDoc.data()?.highVarianceThresholdPaise === "number")
        ? plantDoc.data()!.highVarianceThresholdPaise
        : DEFAULT_HIGH_VARIANCE_THRESHOLD_PAISE;

      let highVarianceAlert = false;
      if (variancePaise >= configuredThresholdPaise) {
        highVarianceAlert = true;
        const notifRef = notificationsRef.doc();
        txn.set(notifRef, {
          type: "HIGH_VARIANCE_ADJUSTMENT",
          itemId: payload.itemId,
          itemCode: item.code ?? "",
          deltaMilli,
          variancePaise,
          reasonCode: payload.reasonCode,
          actorUid: request.auth!.uid,
          createdAt: FieldValue.serverTimestamp(),
          read: false,
        });
      }

      return {
        entryId: entryRef.id,
        balanceAfterMilli: payload.countedOnHandMilli,
        variancePaise,
        highVarianceAlert,
        stockLevel: deriveStockLevel(payload.countedOnHandMilli, item.reorderLevelMilli ?? 0),
      };
    });

    return result;
  }
);
