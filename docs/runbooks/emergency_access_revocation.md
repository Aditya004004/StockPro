# Runbook: Emergency Access Revocation

**Incident Code:** INC-EMERGENCY-ACCESS-REVOCATION  
**Severity:** P0 (Security Incident)  
**Trigger:** Compromised employee credentials, lost shop-floor device, unauthorized access attempts, or immediate employee termination.

---

## 1. Instant Token Revocation (< 2 Minutes)

Revoking refresh tokens invalidates the user's active session immediately upon the next Firebase token refresh (maximum 60 seconds) and blocks subsequent API and Firestore requests.

### Method A: Via Firebase Admin CLI / Script
Run the emergency revocation script:
```bash
node -e "
const admin = require('firebase-admin');
admin.initializeApp();
const targetUid = 'TARGET_USER_UID';
admin.auth().revokeRefreshTokens(targetUid)
  .then(() => admin.auth().updateUser(targetUid, { disabled: true }))
  .then(() => console.log('Successfully revoked and disabled user:', targetUid))
  .catch(err => console.error('Error:', err));
"
```

### Method B: Via In-App Team Management (Plant Manager / Org Admin)
1. Open StockPro → **Settings → Team Management**.
2. Locate the employee row.
3. Tap **Options (⋮) → Deactivate User**.
4. Confirm deactivation in the modal.
   - *Backend executes `deactivateMember.ts` which sets Auth `disabled: true` and calls `revokeRefreshTokens(uid)` immediately.*

---

## 2. Invalidation of Firestore & Outbox Access

1. **Custom Claims Reset:**
   - The user's `orgId` and `plantRoles` custom claims are wiped.
   - Any attempt to read from Firestore will fail security rules (`request.auth.token.plantRoles[plantId] == null`).
2. **Outbox Rejection:**
   - Any mutations currently queued on the revoked device will receive `PERMISSION_DENIED` upon reconnection and will be blocked from writing to the append-only ledger.

---

## 3. Audit Trail Verification

1. Verify that all historical movements posted by the user prior to revocation remain completely intact in `/plants/{plantId}/ledger`.
2. Record the incident, reason, and time of revocation in `/compliance/security_incidents/`.
