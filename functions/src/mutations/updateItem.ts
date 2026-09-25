import * as functions from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { isMasterDataAllowed } from "../domain/stockMath";

const db = getFirestore();

interface UpdateItemPayload {
  orgId: string;
  plantId: string;
  itemId: string;
  name?: string;
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

export const updateItem = functions.onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Authentication required.");
    }

    if (
      typeof payload.orgId !== 'string' || payload.orgId.length === 0 ||
      typeof payload.plantId !== 'string' || payload.plantId.length === 0 ||
      typeof payload.itemId !== 'string' || payload.itemId.length === 0
    ) {
      throw new functions.HttpsError("invalid-argument", "Missing or invalid orgId, plantId, or itemId.");
    }

    const claims = request.auth.token;
    if (claims.orgId !== payload.orgId) {
      throw new functions.HttpsError("permission-denied", "Not a member of this organization.");
    }

    const plantRoles = claims.plantRoles as Record<string, string> | undefined;
    const role = plantRoles?.[payload.plantId];
    if (!role || !isMasterDataAllowed(role)) {
      throw new functions.HttpsError("permission-denied", "You do not have permission to update items in this plant.");
    }

    const itemRef = db
      .collection("organizations").doc(payload.orgId)
      .collection("plants").doc(payload.plantId)
      .collection("items").doc(payload.itemId);

    const doc = await itemRef.get();
    if (!doc.exists) {
      throw new functions.HttpsError("not-found", "Item not found.");
    }

    const updateFields: Record<string, any> = {
      updatedAt: FieldValue.serverTimestamp(),
      updatedBy: request.auth.uid,
    };

    if (payload.name !== undefined) {
      updateFields.name = payload.name.trim();
      updateFields.nameLower = payload.name.trim().toLowerCase();
    }
    if (payload.categoryId !== undefined) updateFields.categoryId = payload.categoryId;
    if (payload.categoryName !== undefined) updateFields.categoryName = payload.categoryName;
    if (payload.unitOfMeasure !== undefined) updateFields.unitOfMeasure = payload.unitOfMeasure;
    if (payload.reorderLevelMilli !== undefined) updateFields.reorderLevelMilli = payload.reorderLevelMilli;
    if (payload.parLevelMilli !== undefined) updateFields.parLevelMilli = payload.parLevelMilli;
    if (payload.unitCostPaise !== undefined) updateFields.unitCostPaise = payload.unitCostPaise;
    if (payload.locationId !== undefined) updateFields.locationId = payload.locationId;
    if (payload.locationCode !== undefined) updateFields.locationCode = payload.locationCode;
    if (payload.barcode !== undefined) updateFields.barcode = payload.barcode;
    if (payload.imagePath !== undefined) updateFields.imagePath = payload.imagePath;

    await itemRef.update(updateFields);

    return {
      itemId: payload.itemId,
      success: true,
    };
  }
);
