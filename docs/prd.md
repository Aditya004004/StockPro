# StockPro — Product Requirements

**Version:** 1.0
**Supersedes:** CaviTrack v1.1 (archived; rebuilt from scratch, not migrated)
**Status:** Draft for sign-off
**Context:** Live deployment inside a manufacturing company

---

## 0. What changed and why we are starting over

CaviTrack v1.1 worked. It was cleanly architected — Clean Architecture, Hilt, `DataResult`, Paging 3, real Firestore security rules, App Check, a working account-deletion flow. The reasons to rebuild are not code quality. They are three structural mismatches that cannot be refactored away:

| CaviTrack assumption | StockPro requirement | Why it forces a rebuild |
|---|---|---|
| One user owns their data (`ownerId == uid`) | An organisation with many plants, many roles, shared records | Every document path, every security rule, and every query changes |
| The client writes stock quantities directly | Quantities are a company's financial record; the client cannot be trusted with them | Inverts the entire write path from client-transaction to server-authoritative |
| Stock is a number you overwrite | Stock is a ledger you append to, and the number is a projection | Different storage model, different audit story, different reconciliation |

Everything worth keeping from CaviTrack is carried into `rules.md` §2 as an explicit "keep" list. Nine known defects from the v1.1 review are carried into §8 of this document as things the rebuild must not reproduce.

---

## 1. Overview

**StockPro is the system of record for physical stock inside a manufacturing plant.**

It tracks four classes of material — Components, Raw Materials, Finished Goods, and Tools — across bin-level locations, and records every movement that changes a quantity: receipts, issues, production output, adjustments, and transfers. It reports on value, consumption, and purchasing over arbitrary date ranges, and it alerts people before something runs out rather than after.

### 1.1 The value proposition

> Know what is running out, what it is worth, and where it sits — from a phone, on the floor, without opening a spreadsheet.

### 1.2 The problems it solves

| Problem | Evidence in the approved designs |
|---|---|
| Stockouts found too late to act on | "Low Stock 18" with a red `+5` delta and "Items need reorder"; a Top 5 Low Stock list on two screens |
| Nobody knows what inventory is worth today | "Inventory Value ₹12.4M" with a six-month trend |
| Movements recorded on paper, keyed into Excel days later | Recent Activity feed of timestamped receipts, issues, and production |
| Items hard to physically locate | Bin codes (A1-01, B2-03) on every row; barcode scanning in the header |
| Reorder timing guessed | Reports table pairing Current Stock against an explicit Reorder Level |
| No operational reporting | Reports tab with Overview / Inventory / Purchases / Usage and four exportable report types |

### 1.3 What "production-ready for a company" adds

This is the part that distinguishes StockPro from a portfolio project, and it is roughly 40% of the total effort. None of it is visible in the screenshots.

- **Server-authoritative quantities.** No client can write a stock level. Ever.
- **Append-only ledger** with nightly reconciliation against the projected balance, and an alert on any drift.
- **Full audit trail.** Every movement names an actor, a timestamp, a reason, and an idempotency key.
- **Role-based access** enforced server-side, verified independently of the client.
- **Backups with a rehearsed restore**, a stated RPO and RTO, and a disaster-recovery runbook.
- **Monitoring and alerting** on sync-queue depth, failed mutations, ledger drift, and error rate.
- **Data ownership and exit.** The company can export everything, in a documented format, at any time.
- **DPDP Act 2023 compliance** for employee personal data (India).
- **A support path** — versioned releases, a rollback plan, and a named owner.

### 1.4 Explicitly out of scope for v1

Financial accounting and GST filing · invoicing and sales orders · payroll · production scheduling and MES · machine signal integration (PLC/OPC-UA) · CAD or drawing management · supplier portals · customer-facing surfaces · multi-company consolidation.

---

## 2. Users

### 2.1 Primary — Inventory Manager

Evidenced directly: *Aditya Maurya · aditya@company.com · Plant 1 | Inventory Manager*.

Owns stock accuracy and reorder decisions for one plant. Moves between a desk and the floor; the phone is the primary device. Opens the app first thing in the morning, scans the KPI row, acts on exceptions. Success is zero unplanned stockouts.

### 2.2 The rest of the org

Evidenced by "Team & Permissions — Manage users and roles".

| Role | Does | Can write | Cannot |
|---|---|---|---|
| **Store Keeper** | Receives deliveries, issues against requisitions, scans bins | Movements (receipt, issue, transfer) | Edit item master data, delete anything, see valuation |
| **Production Supervisor** | Confirms output, checks component availability | Movements (production, issue) | Master data, purchasing |
| **Quality / Inspector** | Logs rejects and holds | Movements (scrap, quarantine) | Everything else |
| **Inventory Manager** | Everything operational for their plant | All movements, item master, reorder levels, POs | Manage users, cross-plant settings |
| **Plant Manager** | Oversight, approvals, team | All of the above, plus team management | Org-level billing/config |
| **Org Admin** | Org setup, plants, roles, billing | Everything | — |

**Design consequence:** the Store Keeper is the highest-volume writer and the least technical user, often working in a low-signal corner of a warehouse. Their flows get the largest targets, the fewest fields, and the most aggressive offline tolerance.

### 2.3 Context

- **Android-first.** One approved screen was rendered on iOS; see `architecture.md` ADR-001 for why v1 ships Android only and how iOS stays cheap to add later.
- **Poor connectivity is normal.** "Last synced: Sep 15, 2026, 8:45 AM" earns its place in Settings only because people routinely work disconnected.
- **Shared devices exist.** A warehouse tablet may pass between shifts. Sessions must be switchable and must never silently attribute one person's movement to another.
- **Locale is India.** ₹ throughout, English first, Hindi likely second.
- **Gloves and dirt.** Targets are 48 dp minimum, 56 dp on Store Keeper paths.

---

## 3. Domain Model

### 3.1 Organisation hierarchy

```
Organisation (the company)
└── Plant (a physical site; "Plant 1")
    ├── Location (zone → rack → bin; "A1-01")
    ├── Item          (the catalogue entry)
    ├── StockLedger   (append-only movement entries)
    ├── PurchaseOrder
    └── DailySnapshot (precomputed reporting rollup)
```

Users hold a role **per plant**, not globally. Someone can be a Store Keeper at Plant 1 and have no access to Plant 2.

### 3.2 Item

| Field | Source | Notes |
|---|---|---|
| `code` | BRG-001, GH-002, CW-003 | Unique within a plant. Printed on the bin label. |
| `name` | "Ball Bearing" | |
| `itemClass` | The four tabs | `COMPONENT · RAW_MATERIAL · FINISHED_GOOD · TOOL` |
| `categoryId` | Mechanical, Electrical, Consumable | Free taxonomy within a class |
| `unitOfMeasure` | pcs, m, L, kg | Canonical; display conversion only |
| `onHand` | The bold right-hand figure | **Projection of the ledger. Never written directly.** |
| `reserved` | Donut "Reserved 156" | Allocated to an order but not yet issued |
| `inProduction` | Donut "In Production 82" | Issued to a work order, not yet consumed |
| `reorderLevel` | Reports table: 50, 30, 100 | Triggers the low-stock alert |
| `parLevel` | The unlabelled "500 pcs" | See §8.1 — **needs a decision and a visible label** |
| `unitCost` | Implied by ₹12.4M valuation | Drives inventory value |
| `locationId` | A1-01 | Primary bin |
| `barcode` | Scan icon | Usually equals `code`; separable |
| `imageUrl` | Product photos on every row | Server generates 128/320/1280 derivatives |

### 3.3 The status question — resolved

The approved donut sums exactly: 980 + 18 + 12 + 156 + 82 = 1,248. That means the five statuses were drawn as **mutually exclusive buckets**.

That model is wrong for a real warehouse. An item can be below its reorder level *and* have 156 units reserved. Forcing exclusivity destroys information.

**Decision: store quantities, derive the label.**

```
stockLevel  = OUT  when onHand <= 0
            = LOW  when onHand <= reorderLevel
            = OK   otherwise

reserved and inProduction are independent quantities, always available
```

The donut becomes a **stacked composition of quantity**, not a count of items, which is both more truthful and more useful — a manager cares that 156 *units* are reserved, not that 156 *items* have some reservation. The design change is described in `design.md` §5.10.

This is the single most consequential modelling decision in the document. Implementing the drawn version would require a migration within the first year.

### 3.4 StockLedger — the spine

Every quantity change is an **append-only entry**. Nothing is ever updated or deleted.

| Field | Notes |
|---|---|
| `type` | `RECEIPT · ISSUE · PRODUCTION · ADJUSTMENT · TRANSFER_IN · TRANSFER_OUT · SCRAP` |
| `deltaMilli` | Signed, in milli-units (see `rules.md` §4) |
| `balanceAfterMilli` | Snapshot of the projection after this entry |
| `itemId`, `plantId`, `orgId` | Scope |
| `reference` | PO number, work order, requisition |
| `reasonCode` | Required on `ADJUSTMENT` and `SCRAP` |
| `actorUid`, `actorName` | Denormalised name, correct *as at* the time |
| `idempotencyKey` | Client-generated UUID; prevents double-posting on retry |
| `createdAt` | Server timestamp, authoritative |

`item.onHand` is a **cached projection** maintained inside the same server transaction that appends the entry. A nightly job re-sums the ledger per item, compares it to the projection, and raises an alert on any mismatch. A correction is itself written as an `ADJUSTMENT` entry — the ledger is never edited to make the numbers agree.

This is what makes the system auditable, and it is the main thing CaviTrack did not do.

### 3.5 Other entities

**PurchaseOrder** (`poNumber`, `supplierId`, `status`, `expectedDate`, lines with `orderedQty`/`receivedQty`) — v1 needs enough to satisfy "3 purchase orders due this week" and the Purchases report. Full authoring is V2.

**Notification** (`type`, `title`, `body`, `entityRef`, `readAt`) — drives the badge count of 3.

**DailySnapshot** (`plantId`, `date`, counts by level, total value, movement totals) — written nightly; reports read these, never the raw ledger.

**AuditEvent** — separate from the ledger, for non-quantity actions: role changes, item master edits, logins, exports, deletions.

---

## 4. Features

### 4.1 Must-have (v1)

**Auth & access**

| ID | Feature |
|---|---|
| A1 | Email/password sign-in; forgot-password |
| A2 | Profile view and edit, avatar upload |
| A3 | Change password |
| A4 | Role-based access, enforced server-side |
| A5 | Plant scoping and switching |
| A6 | Team management: invite, assign role, deactivate |
| A7 | Session handling for shared devices; visible active user |
| A8 | Logout clearing all local data |

**Home**

| ID | Feature |
|---|---|
| H1 | Time-aware greeting |
| H2 | Four KPI tiles with month-over-month deltas |
| H3 | Semantically-aware deltas (Low Stock rising is **bad**, rendered red) |
| H4 | Inventory value with six-month sparkline |
| H5 | Value privacy toggle (role-gated: Store Keepers never see valuation) |
| H6 | Stock composition chart |
| H7 | Four quick actions: Add Item, Receive Stock, Issue Stock, View Reports |
| H8 | Recent activity feed with relative timestamps |
| H9 | Top 5 low-stock list |
| H10 | Upcoming reorders count |
| H11 | Notification centre with unread badge |

**Inventory**

| ID | Feature |
|---|---|
| I1 | Item list, paginated, with thumbnails |
| I2 | Instant local search by name, code, or category |
| I3 | Class tabs: Components / Raw Materials / Finished Goods / Tools |
| I4 | Filter sheet: status, category, location, low-stock-only |
| I5 | Sort: name, code, quantity, last movement, value |
| I6 | Status summary tiles |
| I7 | Barcode and QR scanning |
| I8 | Add and edit item (role-gated) |
| I9 | Item detail with ledger history |
| I10 | Row overflow actions: receive, issue, adjust, edit |

**Movements**

| ID | Feature |
|---|---|
| M1 | Receive stock (with optional PO reference) |
| M2 | Issue stock (with destination and availability check) |
| M3 | Adjust stock (reason code mandatory, role-gated) |
| M4 | Record production output |
| M5 | Transfer between bins |
| M6 | Offline queue with optimistic UI and guaranteed-once posting |
| M7 | Full movement history per item, filterable |

**Reports**

| ID | Feature |
|---|---|
| R1 | Date-range selection |
| R2 | Four sections: Overview / Inventory / Purchases / Usage |
| R3 | Period KPIs: items, value, purchased, issued |
| R4 | Stock composition |
| R5 | Multi-series trend with adjustable window |
| R6 | Top low-stock table with reorder levels |
| R7 | Four generated reports: Inventory, Purchase, Usage, Stock Valuation |
| R8 | PDF and CSV export with share |

**Settings**

| ID | Feature |
|---|---|
| S1 | Profile card |
| S2 | Appearance: Light / Dark / System |
| S3 | Language |
| S4 | Notification preferences (per-type, not one master switch — see §8.2) |
| S5 | Default unit system (display only) |
| S6 | Sync status with last-synced time and pending count |
| S7 | Data export |
| S8 | Clear cache with size estimate |
| S9 | Help, Feedback, Contact Support |
| S10 | About with version and build |

**Platform (invisible, non-negotiable)**

| ID | Feature |
|---|---|
| P1 | Server-authoritative writes; clients cannot write quantities |
| P2 | Append-only ledger with nightly reconciliation |
| P3 | Idempotent mutations |
| P4 | Audit trail for non-quantity actions |
| P5 | Crash and performance monitoring |
| P6 | Automated backups with a rehearsed restore |
| P7 | Three environments: dev, staging, production |
| P8 | Account and data deletion path (Play Store requirement) |

### 4.2 V2 and beyond

| ID | Feature | Why deferred |
|---|---|---|
| V1 | Full purchase-order authoring and approval | v1 needs only PO *references*; authoring is its own product surface |
| V2 | Supplier master data | Follows V1 |
| V3 | Batch / lot / serial traceability | Substantial model change; needed the moment a recall happens |
| V4 | **Tooling depth: shot counters, cavity management, maintenance intervals** | The Tools class is a placeholder in v1. See §7. |
| V5 | Bill of materials and auto-consumption on production | "Motor Assembly (100 pcs)" implies it; needs BOM authoring first |
| V6 | Cycle counting and stock-take mode | High operational value; needs a dedicated flow |
| V7 | Multi-plant consolidated view | Selector implies it; roll-up reporting is non-trivial |
| V8 | iOS client | ADR-001 |
| V9 | Barcode label printing | Printer driver fragmentation |
| V10 | Scheduled report email delivery | |
| V11 | Demand forecasting and auto-reorder | Needs a year of usage history |
| V12 | Web admin console | Bulk import, org config, and reporting are painful on a phone |

---

## 5. User Flows

### Flow 1 — Morning exception review *(the daily loop)*

1. Cold start → session and active plant resolve → Home paints from the local mirror immediately, then revalidates.
2. Greeting resolves by device clock.
3. User reads the KPI row and sees **Low Stock 18**, `+5`, red.
4. **Decision — material?**
   - No → scrolls Recent Activity, confirms the night shift, exits.
   - Yes → taps the Low Stock tile.
5. Inventory opens pre-filtered to low stock, class tab All.
6. Taps a row → Item detail, showing current quantity, reorder level, bin, and recent ledger entries.
7. **Decision — reorder or transfer?**
   - Reorder → Create PO (V2: full authoring; v1: record a PO reference) → Upcoming Reorders increments.
   - Transfer → Transfer movement → ledger entry posted.

*Empty:* "Nothing needs reordering." *Offline:* content renders from the mirror with a banner naming the last sync time.

---

### Flow 2 — Receive stock

1. Home → **Receive Stock**, or Inventory row → **Receive**.
2. **Identify the item:** scan the bin or item barcode, or search.
   - Scan resolves → item loads.
   - No match → "No item matches this code" → *Add item* or *Search manually*.
3. Form shows name, code, image, current quantity, unit — all read-only context.
4. User enters received quantity. Unit is locked to the item's canonical UoM.
5. Optional: PO reference, supplier, destination bin (defaults to the item's bin), note.
6. **Validation:** quantity > 0; within a sane bound; bin exists in this plant; user holds the receive permission.
7. Confirm → optimistic local apply → ledger entry queued with an idempotency key.
8. Server appends the entry and updates the projection in one transaction.
9. Low-stock status clears if crossed; a notification is cancelled.
10. **End state:** confirmation, Home KPIs refresh, activity feed shows the entry.

*Failure:* server rejects → local state rolls back, the reason is shown, the queued entry moves to "Needs attention". Network failure → stays queued, banner shows the pending count, drains on reconnect. **The idempotency key guarantees a retried post after an ambiguous timeout cannot double-receive.**

---

### Flow 3 — Issue stock

1. Home → **Issue Stock**, or from an item.
2. Item identified by scan or search.
3. Form shows **available** quantity, defined as `onHand − reserved`. This is the number that matters and it is not the same as on-hand.
4. User enters quantity and destination (work order, department, line).
5. **Decision — insufficient?** Requested > available → blocked, naming the available figure. Offer partial issue.
6. **Decision — crosses reorder level?** Non-blocking warning: "This drops Gear Housing to 8, below its reorder level of 30."
7. Confirm → optimistic apply → queued → posted.
8. If the new balance is at or below the reorder level, the server raises a low-stock alert and pushes to subscribed roles.
9. **End state:** confirmation; Low Stock KPI may increment.

---

### Flow 4 — Adjust stock *(the one that needs governance)*

Restricted to Inventory Manager and above, because it is the only way to change a quantity without a physical event.

1. Item detail → overflow → **Adjust**.
2. Form shows the system quantity and asks for the **counted** quantity.
3. **Reason code is mandatory:** damaged · lost · found · count correction · expired · quality hold.
4. Optional photo and note. Note required above a configurable variance threshold.
5. Confirm → ledger entry of type `ADJUSTMENT` with the delta, the reason, and the actor.
6. Adjustments above the threshold notify the Plant Manager.
7. **End state:** quantity corrected, adjustment permanently in the ledger and in the audit report.

---

### Flow 5 — Scan to find

1. Inventory → scan icon. First use requests camera permission with a plain-language rationale; denial shows how to enable it and offers manual search.
2. Viewfinder with torch and "Enter code manually".
3. Decode, debounced against repeat reads.
4. **Match in the active plant** → item detail, under two seconds.
   **Match in another plant** → "Found in Plant 2. Switch?"
   **No match** → *Add item* or *Search manually*.

---

### Flow 6 — Month-end report

1. Reports opens on Overview, current month.
2. Range pill → picker → Jun 1–30 → apply. Each section shows its own skeleton; the screen never blanks.
3. Data is read from precomputed daily snapshots, not from the raw ledger.
4. User switches to Purchases or Usage as needed.
5. **Generate Report** → Stock Valuation → PDF or CSV.
6. Generated server-side; under ten seconds it downloads directly, over ten it completes in the background and pushes when ready.
7. **End state:** file handed to the Android share sheet.

---

### Flow 7 — Switch plant

1. Home → plant chip → sheet listing accessible plants.
2. **Decision — unsynced changes?** Yes → "You have 3 unsynced changes. Sync first?" with Sync / Switch anyway / Cancel.
3. Plant-scoped local data is cleared, the new plant's mirror syncs, Home refetches.
4. **End state:** every tab scoped to the new plant; the choice persists across launches.

---

### Flow 8 — Onboarding

1. Launch → session check.
2. Unauthenticated → Sign in.
3. Resolve plant access: one plant → auto-select; several → chooser; none → "No plant assigned. Contact your administrator."
4. First run: request notification permission with a rationale. Camera is deferred to first scan.
5. Initial sync with visible progress; Home paints progressively.

---

## 6. Non-Functional Requirements

| Area | Requirement |
|---|---|
| Cold start to usable Home | < 2.0 s on a mid-range device (from the local mirror) |
| Search response | < 100 ms (local, offline-capable) |
| Scan to item detail | < 2 s |
| List scrolling | 60 fps with 5,000 items |
| Movement posting (online) | < 1.5 s p95 end to end |
| Offline capability | Full read; full write queueing; unbounded queue duration |
| Crash-free sessions | > 99.5% |
| Availability | 99.5% monthly for the backend |
| RPO / RTO | 1 hour / 4 hours |
| Ledger integrity | Zero unexplained drift between ledger sum and projection |
| Supported Android | API 26+ (Android 8.0); target current |
| Install size | < 25 MB |

---

## 7. The Tooling Question *(needs an answer before Phase 3)*

CaviTrack was built for **plastic injection moulding**: molds, cavities, shot life, maintenance intervals. The StockPro designs show general manufacturing — bearings, gear housings, copper wire, control boards, motor assemblies — plus a **Tools** tab.

Two possible readings:

**(a) The company still runs injection moulding**, and Tools is where tooling lives. Then shot counters, cavity blocking, maintenance intervals, and PM alerts are core domain, and the substantial tooling design already produced for CaviTrack v2.0 should be carried into StockPro as a Tools-class extension. This is a significant additional scope — roughly four weeks.

**(b) The company does general manufacturing or assembly**, and Tools means jigs, fixtures, gauges, and hand tools. Then Tools needs only issue/return tracking, calibration due dates, and custody — roughly one week.

**v1 ships the generic Item model with `itemClass = TOOL` and a nullable `toolingProfile` sub-document.** That keeps both doors open at near-zero cost. The decision determines whether Phase 5 is "tooling lifecycle" or "calibration and custody".

**This is question 1 for the business.**

---

## 8. Design Decisions Still Open

Cheap now, expensive after code exists. All of these block Phase 1.

### 8.1 From the approved screens

| # | Issue | Recommendation |
|---|---|---|
| 1 | Brand is **StockPro** on Home, **InventoryPro** in Settings → About | Pick one; it lands in the manifest, the store listing, and every string file |
| 2 | Bottom nav tab 3 is **History** on Inventory, **Reports** on three other screens | Reports. History belongs inside Item detail and Reports, not in a four-slot bar |
| 3 | The unlabelled quantity (500 pcs / 1,000 m / 2,000 kg) | Name it. Recommendation: **par level**, labelled "Par" |
| 4 | Both a header "+ Add Item" **and** a FAB | Keep the FAB; give the header slot to scan, which Store Keepers use far more |
| 5 | "View All" and "See All" used interchangeably | "View all" |
| 6 | Low-stock figures disagree across screens (Ball Bearings 5 vs 320; Copper Wire 10 vs 750; Control Board 12 vs 0) | Mock fixture error; reconcile before the designs are used as build reference |
| 7 | "Ball Bearings" vs "Ball Bearing" | Singular |
| 8 | `₹12.4M` mixes an Indian symbol with Western abbreviation | Locale-aware: **₹1.24 Cr** |
| 9 | Blue is both the action colour and the Reserved status | Move Reserved to teal; blue means "you can act" |

### 8.2 Product gaps behind the screens

| # | Gap | Recommendation |
|---|---|---|
| 10 | Notifications is a single master toggle | Per-type preferences: low stock, out of stock, PO due, large adjustment, sync failure. A single switch means people turn off everything to stop one noisy alert. |
| 11 | No visible adjustment flow, yet quantities will need correcting | Flow 4, role-gated, reason-coded |
| 12 | No reservation management, yet "Reserved 156" is displayed | v1 displays it; V2 makes it actionable |
| 13 | Valuation is visible on Home to everyone | Role-gate it. Inventory value is commercially sensitive. |
| 14 | No stock-take or cycle-count mode | V2, but the ledger model already supports it |

### 8.3 Nine defects from CaviTrack the rebuild must not reproduce

Carried from the v1.1 review, each with its counter-measure in `rules.md`:

1. Collecting all tab data regardless of the visible tab → only the selected tab's data is collected.
2. Detail screens loading once in `init` and showing stale data after an edit → single local source of truth; edits invalidate reactively.
3. Missing composite indexes discovered in production → indexes ship in version control, deployed by CI.
4. Raw exception text shown to users → a mandatory error mapper.
5. One screen hardcoding strings while the rest localised properly → lint gate on hardcoded text.
6. ~17 passthrough use cases → a use case must earn its existence.
7. `listAll()` unpaged, silently under-deleting → all paginated APIs paged.
8. Type scale missing six of fifteen slots → complete token set before any screen is built.
9. Thin tests catching neither known bug → coverage gates and a regression test per fix.

---

## 9. Success Metrics

| Metric | Baseline | Target |
|---|---|---|
| Stockout incidents per month | current | −50% within two months |
| Low-stock items actioned within 48 h | unmeasured | > 80% |
| Movements recorded in-app vs on paper | ~0% | > 95% within one month of rollout |
| Inventory count accuracy vs physical audit | ±5–15% | within ±2% |
| Time to record a receipt | 2–4 min on paper | < 30 s |
| Ledger drift incidents | — | 0 unexplained |
| Crash-free sessions | — | > 99.5% |
| Daily active users among target staff | — | > 90% within one month |

---

## 10. Rollout

| Stage | Scope | Gate |
|---|---|---|
| Internal alpha | Project team, seeded data | Core flows work end to end |
| Pilot | One plant, one item class, parallel with existing process | Two weeks with zero unexplained drift |
| Plant rollout | One plant, all classes, existing process retired | Counts reconcile to ±2% for a full month |
| Multi-plant | Remaining plants | Pilot plant stable for one month |

**Parallel running is mandatory during pilot.** The existing process — spreadsheet or paper — stays live and gets reconciled against StockPro daily. Cutting over without a parallel period is how inventory systems lose a company's stock records.
