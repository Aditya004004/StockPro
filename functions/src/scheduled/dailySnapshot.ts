import { onSchedule } from "firebase-functions/v2/scheduler";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getFirestore, Timestamp } from "firebase-admin/firestore";

const db = getFirestore();

/**
 * dailySnapshot — calculates nightly inventory snapshots per plant.
 * Spec: architecture.md §4.4 & phases.md § Phase 4
 * Runs at 02:00 plant-local. Also callable on-demand.
 */
export async function executeDailySnapshot(orgId: string, plantId: string, targetDateStr?: string) {
  const dateStr = targetDateStr || new Date().toISOString().split("T")[0];
  const plantRef = db.doc(`orgs/${orgId}/plants/${plantId}`);

  // 1. Query all active items for the plant
  const itemsSnapshot = await plantRef.collection("items")
    .where("isActive", "==", true)
    .get();

  let itemCount = 0;
  let inStockCount = 0;
  let lowStockCount = 0;
  let outOfStockCount = 0;
  let reservedMilliTotal = 0;
  let totalValuePaise = 0;

  itemsSnapshot.forEach((doc) => {
    const item = doc.data();
    itemCount++;
    const onHand = item.onHandMilli || 0;
    const reserved = item.reservedMilli || 0;
    const reorder = item.reorderLevelMilli || 0;
    const unitCost = item.unitCostPaise || 0;

    reservedMilliTotal += reserved;
    totalValuePaise += Math.round((onHand / 1000) * unitCost);

    if (onHand <= 0) {
      outOfStockCount++;
    } else if (onHand <= reorder) {
      lowStockCount++;
    } else {
      inStockCount++;
    }
  });

  // 2. Query movements for the day
  const startOfDay = new Date(`${dateStr}T00:00:00.000Z`);
  const endOfDay = new Date(`${dateStr}T23:59:59.999Z`);

  const ledgerSnapshot = await plantRef.collection("ledger")
    .where("createdAt", ">=", Timestamp.fromDate(startOfDay))
    .where("createdAt", "<=", Timestamp.fromDate(endOfDay))
    .get();

  const movementsByType: Record<string, number> = {};
  const quantityByType: Record<string, number> = {};
  let valueReceivedPaise = 0;
  let valueIssuedPaise = 0;

  ledgerSnapshot.forEach((doc) => {
    const entry = doc.data();
    const type = entry.type || "UNKNOWN";
    const delta = Math.abs(entry.deltaMilli || 0);
    const cost = entry.unitCostPaise || 0;

    movementsByType[type] = (movementsByType[type] || 0) + 1;
    quantityByType[type] = (quantityByType[type] || 0) + delta;

    if (type === "RECEIPT" || type === "PRODUCTION") {
      valueReceivedPaise += Math.round((delta / 1000) * cost);
    } else if (type === "ISSUE") {
      valueIssuedPaise += Math.round((delta / 1000) * cost);
    }
  });

  const snapshotData = {
    date: dateStr,
    orgId,
    plantId,
    itemCount,
    inStockCount,
    lowStockCount,
    outOfStockCount,
    reservedMilliTotal,
    totalValuePaise,
    movementsByType,
    quantityByType,
    valueReceivedPaise,
    valueIssuedPaise,
    computedAt: Timestamp.now(),
  };

  // Write to snapshots collection
  await plantRef.collection("snapshots").doc(dateStr).set(snapshotData, { merge: true });
  return snapshotData;
}

export const dailySnapshotScheduled = onSchedule(
  { schedule: "0 2 * * *", timeZone: "Asia/Kolkata" },
  async () => {
    const orgs = await db.collection("orgs").get();
    for (const org of orgs.docs) {
      const plants = await org.ref.collection("plants").get();
      for (const plant of plants.docs) {
        await executeDailySnapshot(org.id, plant.id);
      }
    }
  }
);

export const triggerDailySnapshot = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Auth required.");
  const { orgId, plantId, dateStr } = request.data;
  return await executeDailySnapshot(orgId, plantId, dateStr);
});
