# Digital Personal Data Protection (DPDP) Act 2023 Compliance Assessment

**Jurisdiction:** Republic of India  
**Applicability:** Employee and contractor personal data processed within the StockPro application across Indian manufacturing facilities.  
**Governing Standard:** `docs/architecture.md` §10.5

---

## 1. Data Minimization & Processing Notice (Section 4 & 5)

StockPro adheres strictly to data minimization principles. It only collects employee data strictly necessary for operational authentication and role-based inventory auditing:

| Field | Purpose | Lawful Basis | Storage Location |
|---|---|---|---|
| **Full Name** | Shift audit trail and custody tracking | Legitimate Use (Employment / Inventory Management) | Firebase Auth, Firestore `/team`, local Room DB |
| **Corporate Email** | Authentication & critical low-stock alerts | Legitimate Use (Employment / Operational Communication) | Firebase Auth, Firestore `/team` |
| **Profile Initials / Avatar** | Quick visual identification on shop floor | Legitimate Use | Firebase Auth, Firestore `/team` |
| **Assigned Plant & Role** | RBAC permission enforcement | Legitimate Use (Access Control) | Firebase Auth Custom Claims, Firestore `/team` |

*No personal phone numbers, biometric data, personal addresses, or sensitive personal data (financial, health, demographic) are collected or processed.*

---

## 2. Right to Access, Correction & Erasure (Section 11 & 12)

### 2.1 In-App Access & Correction
Employees can view and update their profile display name directly via **Settings → Edit Profile**. Role and plant assignments are maintained by the authorized Plant Manager.

### 2.2 Account Deactivation vs. Ledger Retention (The 7-Year Invariant)
- **Account Deactivation:** When an employee leaves the organization, their account is deactivated via `deactivateMember.ts`. The Firebase Auth account is disabled, and refresh tokens are revoked.
- **Audit Ledger Invariant:** Under statutory manufacturing compliance and Indian corporate record retention laws, physical stock ledger entries (`/plants/{plantId}/ledger`) must be retained for a minimum of **seven (7) years**. Deactivation does not delete immutable historical movement logs, but anonymizes/archives non-essential profile records.

---

## 3. Data Protection Officer (DPO) & Grievance Redressal

In accordance with Section 8(9) of the DPDP Act 2023, employees and contractors may raise data queries or grievances to:
- **Designated Data Protection Contact:** `dpo@stockpro-manufacturing.com`
- **Statutory Turnaround SLA:** Grievance acknowledgment within 48 hours; resolution within 30 calendar days.
