# StockPro Production Monitoring & Alerting Guide

**Standard:** `docs/architecture.md` §10.3  
**On-Call Tier 1 Response:** 15 Minutes (Critical / Paging) | **Tier 2 Response:** 1 Hour (Warning)

---

## 1. Operational Telemetry & Alert Matrix

| Signal | Threshold / Condition | Severity | Notification Channel | Remediation SLA |
|---|---|---|---|---|
| **Ledger Drift** | `sum(ledger.deltaMilli) != item.onHandMilli` on any item | **P0 (Critical)** | PagerDuty (Immediate Phone Page) + `#alerts-critical` | 15 min acknowledge, 1 hr fix |
| **Function Error Rate** | > 1% over 5-minute rolling window | **P1 (High)** | Slack `#alerts-engineers` + PagerDuty | 30 min |
| **`postMovement` Latency** | p95 latency > 2.0 seconds | **P1 (High)** | Slack `#alerts-engineers` | 1 hr |
| **Outbox Backlog Accumulation** | > 10 items pending in aggregate > 1 hour | **P2 (Medium)** | Slack `#alerts-inventory` | 2 hr |
| **Failed Mutations Spike** | > 5 failures in 1 hour in "Needs Attention" | **P2 (Medium)** | Slack `#alerts-inventory` | 2 hr |
| **Crash-Free Sessions** | < 99.5% over 24-hour observation window | **P1 (High)** | Firebase Crashlytics + Slack `#alerts-mobile` | 2 hr |
| **Auth Failure Spike** | > 3× baseline in 15 minutes | **P1 (High)** | Slack `#alerts-security` | 30 min |
| **Daily Snapshot Missing** | No document in `/snapshots` by 03:30 IST | **P2 (Medium)** | Slack `#alerts-engineers` | 2 hr |
| **Firestore Cost Spike** | > 130% of 7-day trailing average | **P3 (Low)** | Email to Org Admin | 24 hr |

---

## 2. Dashboard Specifications

StockPro maintains 4 core Cloud Monitoring & Grafana dashboards:

### Dashboard 1: Server-Authoritative Write Path
- **Metrics:** `postMovement` request volume, error rate by status code (`200`, `400`, `403`, `409`, `412`, `500`), p50/p95/p99 execution latency.
- **Deduplication Gauge:** Number of idempotent replays detected per hour.

### Dashboard 2: Inventory Ledger & Auto-Healing Health
- **Metrics:** Nightly `reconcileLedger` job execution status, count of items audited, drift incidents detected, auto-healing adjustment documents posted.
- **Drift Zero Counter:** Days since last ledger drift.

### Dashboard 3: Mobile Client Offline Sync & Outbox Depth
- **Metrics:** Active outbox entries across devices (`PENDING`, `POSTING`, `FAILED`), outbox drain rate, WorkManager execution success rate.
- **Needs Attention Roster:** Count of un-discarded failed mutations by plant.

### Dashboard 4: Cloud Resource & Budget Tracking
- **Metrics:** Daily Firestore document reads and writes, Cloud Storage GB consumed, Cloud Functions gigabyte-seconds, monthly budget burn rate.

---

## 3. On-Call Escalation Matrix

### Tier 1: Primary On-Call Engineer
- First responder for all P0/P1 pages.
- Responsible for acknowledging PagerDuty alerts within 15 minutes.
- Evaluates Cloud Logging, checks function status, and executes appropriate runbook.

### Tier 2: Secondary Escalation (Plant Manager / Systems Architect)
- Escalation path if Tier 1 does not acknowledge within 15 minutes or if an incident exceeds 1 hour.
- Authorizes emergency rollbacks, access revocations, or production database restores.
