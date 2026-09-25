import { onSchedule } from "firebase-functions/v2/scheduler";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getFirestore, Timestamp } from "firebase-admin/firestore";

const db = getFirestore();

/**
 * lowStockSweep — sweeps plant inventory and notifies managers of items below reorder level.
 */
export async function executeLowStockSweep(orgId: string, plantId: string) {
  const plantRef = db.doc(`orgs/${orgId}/plants/${plantId}`);
  const itemsSnapshot = await plantRef.collection("items")
    .where("isActive", "==", true)
    .get();

  const lowStockItems: Array<{ id: string; code: string; name: string; onHand: number; reorder: number }> = [];

  itemsSnapshot.forEach((doc) => {
    const item = doc.data();
    const onHand = item.onHandMilli || 0;
    const reorder = item.reorderLevelMilli || 0;
    if (onHand <= reorder && onHand > 0) {
      lowStockItems.push({
        id: doc.id,
        code: item.code,
        name: item.name,
        onHand,
        reorder,
      });
    }
  });

  if (lowStockItems.length > 0) {
    // Check if notification already logged today to prevent duplicate spam
    const today = new Date().toISOString().split("T")[0];
    const notifDocRef = plantRef.collection("notifications").doc(`low-stock-${today}`);

    await notifDocRef.set({
      type: "LOW_STOCK_SWEEP",
      severity: "WARNING",
      itemCount: lowStockItems.length,
      items: lowStockItems.slice(0, 10),
      message: `${lowStockItems.length} items are currently below reorder levels.`,
      createdAt: Timestamp.now(),
      isRead: false,
    }, { merge: true });
  }

  return { scanned: itemsSnapshot.size, lowStockCount: lowStockItems.length };
}

export const lowStockSweepScheduled = onSchedule(
  { schedule: "0 6 * * *", timeZone: "Asia/Kolkata" },
  async () => {
    const orgs = await db.collection("orgs").get();
    for (const org of orgs.docs) {
      const plants = await org.ref.collection("plants").get();
      for (const plant of plants.docs) {
        await executeLowStockSweep(org.id, plant.id);
      }
    }
  }
);

export const triggerLowStockSweep = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Auth required.");
  const { orgId, plantId } = request.data;
  return await executeLowStockSweep(orgId, plantId);
});
