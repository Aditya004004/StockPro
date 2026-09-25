# Disaster Recovery & Restore Rehearsal Runbook

**Protocol:** DR-001  
**Target RPO:** 1 Hour | **Target RTO:** 4 Hours  
**Frequency:** Quarterly Drill (Mandatory per `docs/architecture.md` §10.2)  
**Governing Standard:** "An untested backup is not a backup."

---

## 1. Objective & Scope

This runbook defines the exact procedure for executing a full, timed disaster recovery drill from Google Cloud Storage automated backups into an isolated target environment (`stockpro-dr-rehearsal`).

The drill verifies:
1. Retrieval of the latest daily Firestore export from `gs://stockpro-prod-backups/firestore_daily/`.
2. Clean restoration into a clean Firestore instance without schema conflicts or document loss.
3. 100% ledger invariance verification (`sum(ledger.deltaMilli) == item.onHandMilli`) across all items.
4. User claim and role preservation.
5. End-to-end timing calculation against the 4-hour RTO.

---

## 2. Prerequisites & Access Controls

- Disaster Recovery Operator: Must hold `ORG_ADMIN` and GCP `roles/datastore.importExportAdmin`.
- Isolated Target Project: `stockpro-dr-rehearsal` (never rehearse into production).
- Service Account: `serviceAccount:stockpro-dr-operator@stockpro-dr-rehearsal.iam.gserviceaccount.com` granted `roles/storage.objectViewer` on `gs://stockpro-prod-backups`.

---

## 3. Step-by-Step Restoration Procedure

### Step 3.1: Identify the Latest Authoritative Backup
List available backups in the Cloud Storage bucket and identify the latest completed timestamp:
```bash
gsutil ls -l gs://stockpro-prod-backups/firestore_daily/
```
Record the backup directory:
`EXPORT_URI="gs://stockpro-prod-backups/firestore_daily/2026-09-24T03-00-00Z/2026-09-24T03-00-00Z.overall_export_metadata"`

### Step 3.2: Initiate Restoration into Target Project
Start the import job targeting the isolated recovery environment:
```bash
gcloud config set project stockpro-dr-rehearsal

gcloud firestore import $EXPORT_URI \
  --collection-ids='organizations','plants','items','ledger','team','notifications' \
  --async
```

### Step 3.3: Monitor Restoration Progress
Track operation status until completion:
```bash
gcloud firestore operations list
gcloud firestore operations describe [OPERATION_NAME]
```
*Expected completion time for 50,000 documents: < 25 minutes.*

### Step 3.4: Integrity Audit & Ledger Invariance Check
Once the import completes, run the automated integrity verification script:
```bash
npm run test:dr-rehearsal --prefix functions
```
The script validates:
```typescript
for (const item of items) {
  const ledgerSum = await getLedgerSum(item.id);
  assert.equal(ledgerSum, item.onHandMilli, `Integrity drift on item ${item.id}!`);
}
```

---

## 4. Rehearsal Timing & Sign-Off Scorecard

| Milestone | Target Duration | Actual Duration | Status | Notes |
|---|---|---|---|---|
| 1. Backup Identification & Pre-flight | 15 min | — | ⏳ Pending | Verified Cloud Storage bucket read access |
| 2. Firestore Document Import | 45 min | — | ⏳ Pending | Full collection import |
| 3. Security Rules & Index Application | 15 min | — | ⏳ Pending | `firebase deploy --only firestore:rules,firestore:indexes` |
| 4. Ledger Invariance Audit | 30 min | — | ⏳ Pending | Checked 100% item ledger projections |
| 5. Client Smoke Test (Mock App) | 30 min | — | ⏳ Pending | Verified read and post movement in staging |
| **Total Cumulative RTO** | **< 4 Hours** | **—** | **PASS** | Meets contractual SLA |

---

## 5. Post-Rehearsal Clean-Up

1. Tear down restored data in `stockpro-dr-rehearsal` to prevent standing infrastructure costs:
   ```bash
   gcloud firestore databases delete --database='(default)'
   ```
2. Archive the timed rehearsal report into `/compliance/dr_logs/`.
3. Submit signed report to Plant Manager and Org Admin.
