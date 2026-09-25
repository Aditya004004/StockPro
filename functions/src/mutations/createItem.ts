import * as functions from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { isMasterDataAllowed } from "../domain/stockMath";

const db = getFirestore();

interface CreateItemPayload {
  orgId: string;
  plantId: string;
  code: string;
  name: string;
  itemClass: "COMPONENT" | "RAW_MATERIAL" | "FINISHED_GOOD" | "TOOL";
  categoryId?: string;
  categoryName?: string;
  unitOfMeasure?: string;
  reorderLevelMilli?: number;
  parLevelMilli?: number;
  unitCostPaise?: number;
  locationId?: string;
  locationCode?: string;
  barcode?: string;
  imagePath?: string;
}

export const createItem = functions.onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Authentication required.");
    }

    const payload = request.data as CreateItemPayload;
    if (
      typeof payload.orgId !== 'string' || payload.orgId.length === 0 ||
      typeof payload.plantId !== 'string' || payload.plantId.length === 0 ||
      typeof payload.code !== 'string' || payload.code.length === 0 ||
      typeof payload.name !== 'string' || payload.name.length === 0 ||
      typeof payload.itemClass !== 'string' || !['COMPONENT', 'RAW_MATERIAL', 'FINISHED_GOOD', 'TOOL'].includes(payload.itemClass)
    ) {
      throw new functions.HttpsError("invalid-argument", "Missing required fields (orgId, plantId, code, name, itemClass).");
    }

    const claims = request.auth.token;
    if (claims.orgId !== payload.orgId) {
      throw new functions.HttpsError("permission-denied", "Not a member of this organization.");
    }

    const plantRoles = claims.plantRoles as Record<string, string> | undefined;
    const role = plantRoles?.[payload.plantId];
    if (!role || !isMasterDataAllowed(role)) {
      throw new functions.HttpsError("permission-denied", "You do not have permission to create items in this plant.");
    }

    const itemsRef = db
      .collection("organizations").doc(payload.orgId)
      .collection("plants").doc(payload.plantId)
      .collection("items");

    const codeLower = payload.code.trim().toLowerCase();
    const nameLower = payload.name.trim().toLowerCase();

    const newItemRef = itemsRef.doc();
    const newItemData = {
      id: newItemRef.id,
      code: payload.code.trim(),
      codeLower,
      name: payload.name.trim(),
      nameLower,
      itemClass: payload.itemClass,
      categoryId: payload.categoryId ?? "",
      categoryName: payload.categoryName ?? "",
      unitOfMeasure: payload.unitOfMeasure ?? "PCS",
      onHandMilli: 0,
      reservedMilli: 0,
      inProductionMilli: 0,
      reorderLevelMilli: payload.reorderLevelMilli ?? 0,
      parLevelMilli: payload.parLevelMilli ?? 0,
      unitCostPaise: payload.unitCostPaise ?? 0,
      locationId: payload.locationId ?? "",
      locationCode: payload.locationCode ?? "",
      barcode: payload.barcode ?? null,
      imagePath: payload.imagePath ?? null,
      isActive: true,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
      createdBy: request.auth.uid,
      updatedBy: request.auth.uid,
      lastMovementAt: null,
    };

    await db.runTransaction(async (txn) => {
      // Atomically check code uniqueness per plant
      const existing = await txn.get(itemsRef.where("codeLower", "==", codeLower).limit(1));
      if (!existing.empty) {
        throw new functions.HttpsError("already-exists", `An item with code '${payload.code}' already exists in this plant.`);
      }
      txn.set(newItemRef, newItemData);
    });

    return {
      itemId: newItemRef.id,
      code: payload.code.trim(),
      name: payload.name.trim(),
    };
  }
);
