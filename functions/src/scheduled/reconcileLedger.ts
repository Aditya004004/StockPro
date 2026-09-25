import { onSchedule } from "firebase-functions/v2/scheduler";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getFirestore, Timestamp, FieldValue } from "firebase-admin/firestore";
import { v4 as uuidv4 } from "uuid";

const db = getFirestore();

export interface ReconciliationReport {
  orgId: string;
  plantId: string;
  scannedItems: number;
  healthyItems: number;
  driftCount: number;
  driftDetails: Array<{
    itemId: string;
    itemCode: string;
    projectedOnHandMilli: number;
    calculatedLedgerSumMilli: number;
    driftMilli: number;
    autoHealed: boolean;
  }>;
  executedAt: string;
}

/**
 * reconcileLedger — nightly mathematical invariance audit.
 * Re-sums every item's ledger deltaMilli and compares to item.onHandMilli.
 * Any drift is immediately alerted to notifications and auto-healed via an authoritative ADJUSTMENT entry.
 */
export async function executeReconcileLedger(
  orgId: string,
  plantId: string,
  autoHeal: boolean = true
): Promise<ReconciliationReport> {
  const plantRef = db.doc(`orgs/${orgId}/plants/${plantId}`);
  const itemsSnapshot = await plantRef.collection("items").get();

  const report: ReconciliationReport = {
    orgId,
    plantId,
    scannedItems: itemsSnapshot.size,
    healthyItems: 0,
    driftCount: 0,
    driftDetails: [],
    executedAt: new Date().toISOString(),
  };

  for (const itemDoc of itemsSnapshot.docs) {
    const item = itemDoc.data();
    const itemId = itemDoc.id;
    const projectedOnHand = item.onHandMilli || 0;

    // Sum all ledger entries for this item
    const ledgerSnapshot = await plantRef.collection("ledger")
      .where("itemId", "==", itemId)
      .get();

    let calculatedSum = 0;
    ledgerSnapshot.forEach((doc) => {
      const entry = doc.data();
      calculatedSum += (entry.deltaMilli || 0);
    });

    if (calculatedSum !== projectedOnHand) {
      report.driftCount++;
      const driftMilli = calculatedSum - projectedOnHand;
      let healed = false;

      // 1. Post immediate high-priority drift alert
      await plantRef.collection("notifications").add({
        type: "LEDGER_DRIFT_ALERT",
        severity: "CRITICAL",
        itemId,
        itemCode: item.code,
        projectedOnHand,
        calculatedSum,
        driftMilli,
        message: `Ledger drift detected for item ${item.code}: projected ${projectedOnHand} milli vs ledger ${calculatedSum} milli (drift: ${driftMilli} milli).`,
        createdAt: Timestamp.now(),
        isRead: false,
      });

      // 2. Auto-heal via authoritative ADJUSTMENT if enabled
      if (autoHeal) {
        await db.runTransaction(async (transaction) => {
          const freshItemDoc = await transaction.get(itemDoc.ref);
          if (!freshItemDoc.exists) return;

          const balanceAfter = calculatedSum; // Authoritative balance is the ledger sum
          const correctionDelta = -driftMilli;

          // Append reconciliation ledger entry
          const ledgerRef = plantRef.collection("ledger").doc();
          transaction.set(ledgerRef, {
            orgId,
            plantId,
            itemId,
            type: "ADJUSTMENT",
            deltaMilli: correctionDelta,
            balanceAfterMilli: balanceAfter,
            unitCostPaise: item.unitCostPaise || 0,
            reference: "AUTO-RECONCILE",
            reasonCode: "DRIFT_CORRECTION",
            note: `Automated ledger reconciliation: corrected drift of ${driftMilli} milli`,
            actorUid: "system-reconcile",
            actorName: "System Reconciler",
            idempotencyKey: uuidv4(),
            clientCreatedAt: Timestamp.now(),
            createdAt: FieldValue.serverTimestamp(),
          });

          // Correct item projection
          transaction.update(itemDoc.ref, {
            onHandMilli: calculatedSum,
            lastMovementAt: FieldValue.serverTimestamp(),
            updatedAt: FieldValue.serverTimestamp(),
          });
        });
        healed = true;
      }

      report.driftDetails.push({
        itemId,
        itemCode: item.code || itemId,
        projectedOnHandMilli: projectedOnHand,
        calculatedLedgerSumMilli: calculatedSum,
        driftMilli,
        autoHealed: healed,
      });
    } else {
      report.healthyItems++;
    }
  }

  // Save reconciliation log
  await plantRef.collection("reconciliation_logs").add({
    ...report,
    recordedAt: Timestamp.now(),
  });

  return report;
}

export const reconcileLedgerScheduled = onSchedule(
  { schedule: "30 2 * * *", timeZone: "Asia/Kolkata" },
  async () => {
    const orgs = await db.collection("orgs").get();
    for (const org of orgs.docs) {
      const plants = await org.ref.collection("plants").get();
      for (const plant of plants.docs) {
        await executeReconcileLedger(org.id, plant.id, true);
      }
    }
  }
);

export const triggerReconcileLedger = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Auth required.");
  const { orgId, plantId, autoHeal } = request.data;
  return await executeReconcileLedger(orgId, plantId, autoHeal !== false);
});
