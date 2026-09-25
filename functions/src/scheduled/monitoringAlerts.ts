import * as scheduler from "firebase-functions/v2/scheduler";
import * as https from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import { getFirestore } from "firebase-admin/firestore";

const db = getFirestore();

export interface HealthCheckResult {
  healthy: boolean;
  ledgerDriftAlerts: number;
  pendingOutboxCount: number;
  failedMutationsCount: number;
  snapshotJobHealth: "OK" | "MISSED" | "FAILED";
  alertsTriggered: string[];
}

/**
 * Evaluates core operational signals per docs/architecture.md §10.3:
 * - Ledger drift: immediate page
 * - Outbox items pending > 1h (aggregate > 10)
 * - Failed mutations in "Needs attention" (> 5 / hr)
 * - Daily snapshot completion health
 */
export async function runMonitoringHealthCheck(): Promise<HealthCheckResult> {
  const alertsTriggered: string[] = [];
  const oneHourAgo = Date.now() - (60 * 60 * 1000);

  // 1. Check for unresolved ledger drift alerts
  const driftSnapshot = await db.collection("system_alerts")
    .where("type", "==", "LEDGER_DRIFT")
    .where("resolved", "==", false)
    .get();

  const ledgerDriftAlerts = driftSnapshot.size;
  if (ledgerDriftAlerts > 0) {
    alertsTriggered.push(`CRITICAL PAGER: ${ledgerDriftAlerts} unresolved ledger drift event(s) detected! Immediate investigation required.`);
    logger.error("CRITICAL LEDGER DRIFT ALERT PAGING ON-CALL", { count: ledgerDriftAlerts });
  }

  // 2. Check for failed outbox entries in last hour (> 5 threshold)
  const failedMutationsSnapshot = await db.collection("failed_outbox")
    .where("failedAt", ">=", oneHourAgo)
    .get();

  const failedMutationsCount = failedMutationsSnapshot.size;
  if (failedMutationsCount > 5) {
    alertsTriggered.push(`WARNING: Spike in failed mutations (${failedMutationsCount} in last hour). Check for connectivity or validation changes.`);
    logger.warn("Spike in failed mutations", { count: failedMutationsCount });
  }

  // 3. Check for stale pending outbox items (> 10 items pending > 1h)
  const pendingSnapshot = await db.collection("outbox_monitor")
    .where("status", "==", "PENDING")
    .where("createdAt", "<=", oneHourAgo)
    .get();

  const pendingOutboxCount = pendingSnapshot.size;
  if (pendingOutboxCount > 10) {
    alertsTriggered.push(`WARNING: Outbox backlog accumulated (${pendingOutboxCount} items pending > 1 hour). Check worker drain health.`);
    logger.warn("Outbox backlog alert", { count: pendingOutboxCount });
  }

  // 4. Verify daily snapshot completion
  const todayDateStr = new Date().toISOString().split("T")[0];
  const snapshotDoc = await db.collection("snapshots").doc(todayDateStr).get();
  const snapshotJobHealth = snapshotDoc.exists ? "OK" : "MISSED";

  if (snapshotJobHealth === "MISSED") {
    // Only alert if current time is past 03:00 IST
    const currentHourUtc = new Date().getUTCHours();
    if (currentHourUtc >= 22 || currentHourUtc <= 12) {
      alertsTriggered.push(`ALERT: Daily inventory snapshot for ${todayDateStr} is missing or delayed.`);
      logger.warn("Daily snapshot job health check failed", { date: todayDateStr });
    }
  }

  const healthy = alertsTriggered.length === 0;

  // Log summary
  logger.info("Operational Monitoring Health Check complete", {
    healthy,
    ledgerDriftAlerts,
    failedMutationsCount,
    pendingOutboxCount,
    snapshotJobHealth,
    alertsCount: alertsTriggered.length,
  });

  return {
    healthy,
    ledgerDriftAlerts,
    pendingOutboxCount,
    failedMutationsCount,
    snapshotJobHealth,
    alertsTriggered,
  };
}

/**
 * Recurring health check every 15 minutes.
 */
export const monitoringHealthCheckScheduled = scheduler.onSchedule(
  {
    schedule: "*/15 * * * *",
    timeZone: "Asia/Kolkata",
    memory: "256MiB",
  },
  async () => {
    await runMonitoringHealthCheck();
  }
);

/**
 * Callable trigger for dashboard monitoring queries.
 */
export const triggerMonitoringHealthCheck = https.onCall(
  { enforceAppCheck: true },
  async (request) => {
    if (!request.auth) {
      throw new https.HttpsError("unauthenticated", "Authentication required.");
    }
    return await runMonitoringHealthCheck();
  }
);
