# Runbook: Investigate and Resolve Ledger Drift

**Incident Code:** INC-LEDGER-DRIFT  
**Severity:** P0 (Critical — Immediate Page)  
**Trigger:** `reconcileLedger` detected `sum(ledger.deltaMilli) != item.onHandMilli` on one or more items.

---

## 1. Immediate Triage (0–15 Minutes)

1. **Acknowledge Alert:** Acknowledge the PagerDuty incident.
2. **Identify Affected Item(s):**
   - Query `/system_alerts` in Firestore where `type == "LEDGER_DRIFT"` and `resolved == false`.
   - Note `plantId`, `itemId`, `onHandMilli`, `calculatedSumMilli`, and `varianceMilli`.
3. **Check Auto-Healing Status:**
   - Did `reconcileLedger` post an automated `ADJUSTMENT` movement?
   - If yes: The balance is temporarily healed, but root-cause investigation is required.
   - If no: Proceed to step 2 to freeze direct writes on the item.

---

## 2. Investigation Protocol (15–45 Minutes)

### Step 2.1: Pull Complete Movement Ledger
Export all ledger entries for the affected item:
```bash
firebase functions:shell
> getLedgerEntries({ orgId: "org_acme", plantId: "plant_01", itemId: "item_affected" })
```

### Step 2.2: Identify Root Cause Anomaly
Check for the 4 known causes of ledger drift:
1. **Un-audited Direct Write:** Did an administrator execute a direct update on the Firestore document bypassing Cloud Functions? (Check Cloud Audit Logs for `google.firestore.v1.Firestore.UpdateDocument` on `/items/{itemId}`).
2. **Partial Transaction Failure:** Did a Firestore transaction timeout mid-execution?
3. **Idempotency Key Collision:** Did an identical idempotency key get reused across different movement payloads?
4. **Offline Concurrency Clamping:** Did two offline issues exceed zero on arrival and apply inconsistent reconciliation?

---

## 3. Remediation & Reconciliation

1. **Verify Physical Stock on Shop Floor:**
   - Contact plant Store Keeper to perform an immediate physical cycle count of the item.
2. **Post Authoritative Audit Entry:**
   - Post an explicit `ADJUSTMENT` movement with reference `"INC-LEDGER-DRIFT-RESOLVED"` and detailed reason note explaining the findings.
3. **Mark Alert Resolved:**
   - Update the `/system_alerts` document with resolution notes, investigator ID, and timestamp.
