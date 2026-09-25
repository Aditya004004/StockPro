import * as functions from "firebase-functions/v2/https";
import { getMessaging } from "firebase-admin/messaging";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

const db = getFirestore();
const messaging = getMessaging();

interface PushNotificationPayload {
  orgId: string;
  plantId: string;
  targetTopic?: string; // e.g. "plant_01" or "plant_01_INVENTORY_MANAGER"
  type: "LOW_STOCK" | "OUT_OF_STOCK" | "PO_DUE" | "LARGE_ADJUSTMENT" | "SYNC_FAILURE" | "AUDIT_DRIFT";
  title: string;
  body: string;
  entityRef?: string;
}

export const sendPushNotification = functions.onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Authentication required.");
    }

    const payload = request.data as PushNotificationPayload;
    if (!payload.orgId || !payload.plantId || !payload.type || !payload.title || !payload.body) {
      throw new functions.HttpsError("invalid-argument", "Missing required notification fields.");
    }

    const topic = payload.targetTopic || `plant_${payload.plantId}`;

    // 1. Dispatch FCM Push Notification (architecture.md §2.2)
    const message = {
      topic,
      notification: {
        title: payload.title,
        body: payload.body,
      },
      data: {
        type: payload.type,
        plantId: payload.plantId,
        entityRef: payload.entityRef || "",
        click_action: "FLUTTER_NOTIFICATION_CLICK",
      },
    };

    let messageId: string | null = null;
    let fcmError: string | null = null;
    try {
      messageId = await messaging.send(message);
    } catch (err: any) {
      fcmError = err?.message || "Unknown FCM dispatch error";
      functions.logger.error(`Failed to dispatch FCM to topic ${topic}: ${fcmError}`, err);
    }

    // 2. Persist notification in Firestore notifications sub-collection (architecture.md §4.1)
    const notifRef = db
      .collection("organizations").doc(payload.orgId)
      .collection("plants").doc(payload.plantId)
      .collection("notifications").doc();

    await notifRef.set({
      id: notifRef.id,
      type: payload.type,
      title: payload.title,
      body: payload.body,
      entityRef: payload.entityRef || null,
      isRead: false,
      fcmMessageId: messageId,
      fcmDeliveryStatus: messageId ? "DELIVERED" : "FAILED",
      fcmError: fcmError,
      createdAt: FieldValue.serverTimestamp(),
      createdBy: request.auth.uid,
    });

    return {
      success: true,
      notificationId: notifRef.id,
      fcmMessageId: messageId,
      fcmDeliveryStatus: messageId ? "DELIVERED" : "FAILED",
      fcmError: fcmError,
    };
  }
);
