import * as functions from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { isTeamManagementAllowed } from "../domain/stockMath";

const db = getFirestore();
const auth = getAuth();

interface DeactivateMemberPayload {
  orgId: string;
  plantId: string;
  memberUid: string;
}

export const deactivateMember = functions.onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Authentication required.");
    }

    const payload = request.data as DeactivateMemberPayload;
    if (!payload.orgId || !payload.plantId || !payload.memberUid) {
      throw new functions.HttpsError("invalid-argument", "Missing required fields (orgId, plantId, memberUid).");
    }

    const claims = request.auth.token;
    if (claims.orgId !== payload.orgId) {
      throw new functions.HttpsError("permission-denied", "Not a member of this organization.");
    }

    const callerPlantRoles = claims.plantRoles as Record<string, string> | undefined;
    const callerRole = callerPlantRoles?.[payload.plantId];
    if (!callerRole || !isTeamManagementAllowed(callerRole)) {
      throw new functions.HttpsError("permission-denied", "Only Plant Managers and Org Admins can deactivate members.");
    }

    if (payload.memberUid === request.auth.uid) {
      throw new functions.HttpsError("failed-precondition", "You cannot deactivate your own account.");
    }

    // Disable in Firebase Auth
    await auth.updateUser(payload.memberUid, { disabled: true });
    await auth.revokeRefreshTokens(payload.memberUid);

    // Update organization member doc
    const memberRef = db
      .collection("organizations").doc(payload.orgId)
      .collection("members").doc(payload.memberUid);

    await memberRef.set(
      {
        status: "DEACTIVATED",
        deactivatedAt: FieldValue.serverTimestamp(),
        deactivatedBy: request.auth.uid,
      },
      { merge: true }
    );

    // Record audit event preserving explicit ledger history statement (phases.md § Phase 5)
    await db
      .collection("organizations").doc(payload.orgId)
      .collection("auditEvents").add({
        type: "MEMBER_DEACTIVATED",
        actorUid: request.auth.uid,
        targetUid: payload.memberUid,
        plantId: payload.plantId,
        historyStatement: "User access revoked. All movements, cycle counts, and audit logs created by this user remain preserved in the ledger.",
        createdAt: FieldValue.serverTimestamp(),
      });

    return {
      success: true,
      memberUid: payload.memberUid,
      status: "DEACTIVATED",
    };
  }
);
