# Runbook: Clear a Stuck Outbox on a User's Device

**Incident Code:** INC-STUCK-OUTBOX  
**Severity:** P2 (Medium)  
**Trigger:** User reports pending sync banner not clearing, or operational monitoring detects `outbox items pending > 1 h`.

---

## 1. Diagnosis Protocol

A mobile outbox may become stuck due to:
1. **Poisoned Payload:** Corrupted JSON payload causing `OutboxWorker` to fail deserialization.
2. **Expired Authentication:** User session revoked or expired while offline.
3. **Repeated 4xx Precondition:** Mutation constantly rejected by server but not properly transitioning to `STATUS_FAILED`.
4. **WorkManager Job Constraint Block:** OS battery optimization preventing background worker execution.

---

## 2. In-App Resolution Steps (Operator Self-Service)

Instruct the shop-floor user to follow these steps inside StockPro:

1. **Tap the Sync Banner:**
   - Tap the orange or red sync banner at the top of Home or Inventory screen.
   - If "Needs Attention" modal appears, tap **Retry All** or **Discard** on invalid entries.
2. **Manual Force Sync:**
   - Navigate to **Settings → Backup & Local Sync**.
   - Tap **Sync Now**.
3. **Inspect Cache & Connection:**
   - Ensure device Wi-Fi/LTE is active.
   - If sync remains stuck, navigate to **Settings → Clear Cache** (this clears image thumbnails and transient cache without touching the Room outbox).

---

## 3. Administrator Remote Resolution

If the user cannot resolve via UI:

1. **Check Server-Side Rejections:**
   - Query Firestore `/failed_outbox` for the user's `uid`.
   - If server rejected due to insufficient permissions or invalid parameters, instruct user to Discard the movement.
2. **Inspect Room Outbox Diagnostics (Remote Support):**
   - Ask the user to tap **Settings → Help & Feedback → Send Diagnostic Report**.
   - Review outbox state dump (`outbox_dump.json`) in support portal.
3. **Nuclear Option (Only if Outbox Database is Corrupted):**
   - *Warning: Unposted local mutations will be lost.*
   - Instruct user to go to Android **Settings → Apps → StockPro → Storage → Clear Storage**.
   - Upon next sign-in, StockPro will perform a clean initial sync from Firestore.
   - Re-enter any physical movements that were in progress.
