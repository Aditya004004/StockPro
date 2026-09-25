import * as functions from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { isDeactivationAllowed } from "../domain/stockMath";

const db = getFirestore();

interface DeactivateItemPayload {
  orgId: string;
  plantId: string;
  itemId: string;
}

export const deactivateItem = functions.onCall(
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
    if (!role || !isDeactivationAllowed(role)) {
      throw new functions.HttpsError("permission-denied", "Only Plant Managers and Org Admins can deactivate items.");
    }

    const itemRef = db
      .collection("organizations").doc(payload.orgId)
      .collection("plants").doc(payload.plantId)
      .collection("items").doc(payload.itemId);

    const doc = await itemRef.get();
    if (!doc.exists) {
      throw new functions.HttpsError("not-found", "Item not found.");
    }

    await itemRef.update({
      isActive: false,
      updatedAt: FieldValue.serverTimestamp(),
      updatedBy: request.auth.uid,
    });

    return {
      itemId: payload.itemId,
      isActive: false,
    };
  }
);
