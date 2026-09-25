# Runbook: Roll Back a Bad Cloud Function Deploy

**Incident Code:** INC-FUNCTION-ROLLBACK  
**Severity:** P0/P1  
**Trigger:** Cloud Functions error rate > 1%, elevated latency, or breaking validation bugs introduced by a new deployment.

---

## 1. Zero-Downtime Rollback Procedure

Google Cloud Functions v2 runs on Google Cloud Run. Every deployment creates an immutable revision. Rollback can be completed in < 60 seconds without re-building or re-uploading code.

### Step 1.1: Identify the Previous Healthy Revision
List recent revisions for the affected function (e.g., `postMovement`):
```bash
gcloud run revisions list \
  --service=postmovement \
  --region=asia-south1 \
  --project=stockpro-prod
```
Note the name of the previous healthy revision (e.g., `postmovement-00042-xyz`).

### Step 1.2: Shift 100% Traffic to the Healthy Revision
Direct all incoming traffic immediately back to the healthy revision:
```bash
gcloud run services update-traffic postmovement \
  --to-revisions=postmovement-00042-xyz=100 \
  --region=asia-south1 \
  --project=stockpro-prod
```
*Effect: Immediate. All in-flight and incoming calls are served by the previous revision.*

---

## 2. Git & CI/CD Reconciliation

1. **Revert the Commit:**
   ```bash
   git revert [BAD_COMMIT_SHA] -m "Revert bad function release"
   git push origin main
   ```
2. **Deploy Reverted Codebase via Firebase CLI:**
   ```bash
   firebase deploy --only functions:postMovement --project stockpro-prod
   ```
3. **Verify Health:**
   - Monitor Cloud Functions dashboard for 15 minutes.
   - Ensure error rate drops back to < 0.1% and p95 latency returns to normal.
