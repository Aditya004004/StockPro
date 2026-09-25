import * as functions from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { isTeamManagementAllowed } from "../domain/stockMath";

const db = getFirestore();
const auth = getAuth();

interface UpdateMemberRolePayload {
  orgId: string;
  plantId: string;
  memberUid: string;
  newRole: "STORE_KEEPER" | "PRODUCTION_SUPERVISOR" | "QA" | "INVENTORY_MANAGER" | "PLANT_MANAGER" | "ORG_ADMIN";
}

export const updateMemberRole = functions.onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Authentication required.");
    }

    const payload = request.data as UpdateMemberRolePayload;
    if (!payload.orgId || !payload.plantId || !payload.memberUid || !payload.newRole) {
      throw new functions.HttpsError("invalid-argument", "Missing required fields (orgId, plantId, memberUid, newRole).");
    }

    const claims = request.auth.token;
    if (claims.orgId !== payload.orgId) {
      throw new functions.HttpsError("permission-denied", "Not a member of this organization.");
    }

    const callerPlantRoles = claims.plantRoles as Record<string, string> | undefined;
    const callerRole = callerPlantRoles?.[payload.plantId];
    if (!callerRole || !isTeamManagementAllowed(callerRole)) {
      throw new functions.HttpsError("permission-denied", "Only Plant Managers and Org Admins can update roles.");
    }

    const targetUser = await auth.getUser(payload.memberUid);
    const existingClaims = (targetUser.customClaims || {}) as Record<string, any>;
    const currentPlantRoles = (existingClaims.plantRoles || {}) as Record<string, string>;
    const oldRole = currentPlantRoles[payload.plantId] ?? "UNKNOWN";
    const updatedPlantRoles = { ...currentPlantRoles, [payload.plantId]: payload.newRole };

    // Update custom claims
    await auth.setCustomUserClaims(payload.memberUid, {
      ...existingClaims,
      plantRoles: updatedPlantRoles,
    });

    // Force client re-authentication so new claims take immediate effect (phases.md § Phase 5)
    await auth.revokeRefreshTokens(payload.memberUid);

    // Update organization member doc
    const memberRef = db
      .collection("organizations").doc(payload.orgId)
      .collection("members").doc(payload.memberUid);

    await memberRef.set(
      {
        plantRoles: updatedPlantRoles,
        updatedAt: FieldValue.serverTimestamp(),
        updatedBy: request.auth.uid,
      },
      { merge: true }
    );

    // Record non-quantity action in immutable auditEvents (architecture.md §4.1 / prd.md §3.5)
    await db
      .collection("organizations").doc(payload.orgId)
      .collection("auditEvents").add({
        type: "ROLE_CHANGED",
        actorUid: request.auth.uid,
        targetUid: payload.memberUid,
        plantId: payload.plantId,
        oldRole,
        newRole: payload.newRole,
        createdAt: FieldValue.serverTimestamp(),
      });

    return {
      success: true,
      memberUid: payload.memberUid,
      newRole: payload.newRole,
    };
  }
);
