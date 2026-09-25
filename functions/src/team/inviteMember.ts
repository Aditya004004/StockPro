import * as functions from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { isTeamManagementAllowed } from "../domain/stockMath";

const db = getFirestore();
const auth = getAuth();

interface InviteMemberPayload {
  orgId: string;
  plantId: string;
  email: string;
  displayName: string;
  role: "STORE_KEEPER" | "PRODUCTION_SUPERVISOR" | "QA" | "INVENTORY_MANAGER" | "PLANT_MANAGER" | "ORG_ADMIN";
}

export const inviteMember = functions.onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Authentication required.");
    }

    const payload = request.data as InviteMemberPayload;
    if (!payload.orgId || !payload.plantId || !payload.email || !payload.role) {
      throw new functions.HttpsError("invalid-argument", "Missing required fields (orgId, plantId, email, role).");
    }

    const claims = request.auth.token;
    if (claims.orgId !== payload.orgId) {
      throw new functions.HttpsError("permission-denied", "Not a member of this organization.");
    }

    const callerPlantRoles = claims.plantRoles as Record<string, string> | undefined;
    const callerRole = callerPlantRoles?.[payload.plantId];
    if (!callerRole || !isTeamManagementAllowed(callerRole)) {
      throw new functions.HttpsError("permission-denied", "Only Plant Managers and Org Admins can manage team members.");
    }

    const emailTrimmed = payload.email.trim().toLowerCase();

    // Check if user already exists in Firebase Auth or create placeholder
    let userRecord;
    try {
      userRecord = await auth.getUserByEmail(emailTrimmed);
    } catch {
      // User doesn't exist yet, create user account
      userRecord = await auth.createUser({
        email: emailTrimmed,
        displayName: payload.displayName.trim() || emailTrimmed.split("@")[0],
      });
    }

    // Set or merge custom claims
    const existingClaims = (userRecord.customClaims || {}) as Record<string, any>;
    const currentPlantRoles = (existingClaims.plantRoles || {}) as Record<string, string>;
    const updatedPlantRoles = { ...currentPlantRoles, [payload.plantId]: payload.role };

    await auth.setCustomUserClaims(userRecord.uid, {
      ...existingClaims,
      orgId: payload.orgId,
      plantRoles: updatedPlantRoles,
    });

    // Update organization member document (architecture.md §4.1)
    const memberRef = db
      .collection("organizations").doc(payload.orgId)
      .collection("members").doc(userRecord.uid);

    await memberRef.set(
      {
        uid: userRecord.uid,
        email: emailTrimmed,
        displayName: payload.displayName.trim() || emailTrimmed.split("@")[0],
        status: "ACTIVE",
        plantRoles: updatedPlantRoles,
        updatedAt: FieldValue.serverTimestamp(),
        invitedBy: request.auth.uid,
      },
      { merge: true }
    );

    // Audit event (prd.md §3.5)
    await db
      .collection("organizations").doc(payload.orgId)
      .collection("auditEvents").add({
        type: "MEMBER_INVITED",
        actorUid: request.auth.uid,
        targetUid: userRecord.uid,
        targetEmail: emailTrimmed,
        assignedRole: payload.role,
        plantId: payload.plantId,
        createdAt: FieldValue.serverTimestamp(),
      });

    return {
      uid: userRecord.uid,
      email: emailTrimmed,
      displayName: payload.displayName.trim() || emailTrimmed.split("@")[0],
      role: payload.role,
    };
  }
);
