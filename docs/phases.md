# StockPro — Implementation Roadmap

**Version:** 1.0
**Baseline:** greenfield; CaviTrack archived for reference only
**Target:** live deployment in a manufacturing company

**Team assumption:** 1–2 Android engineers with part-time backend capacity. If it is one person, multiply every duration by roughly 1.6 — the estimates assume some parallelism between client and functions work.

---

## Reading the plan

Phases 1–2 build the machinery; Phases 3–5 build features on top of it as vertical slices. That order is deliberate: the write path, offline outbox, and ledger are the hard parts, and building screens first means retrofitting them later, which never goes well.

**Phase 7 is not optional.** "Production-ready for a company" is roughly 40% infrastructure, governance, and operational readiness that no screenshot shows. Skipping it means shipping something that works in a demo and fails in a warehouse.

---

## Phase 0 — Decisions (1 week, blocking)

Nothing starts until these are closed. Each is one sentence now and a multi-file refactor later.

**Product**
- [ ] Name: StockPro or InventoryPro — lands in the manifest, the store listing, and every string
- [ ] **Tooling scope** (`prd.md` §7): is this still injection moulding? Determines whether Phase 5 is four weeks or one
- [ ] Nav tab 3: Reports or History
- [ ] Par level: confirm what the unlabelled "500 pcs" means, and label it
- [ ] Which roles see inventory valuation
- [ ] Launch locales: English only, or English + Hindi
- [ ] Plant count at launch, item count per plant, movement volume per day

**Technical**
- [ ] Confirm ADR-001 (Android-only v1). **If iOS is contractual, say so now** — it changes the stack decision, not just the timeline
- [ ] Confirm ADR-002 (server-authoritative writes). This is the biggest single cost and the biggest single integrity gain
- [ ] Google Cloud billing account, org policy, and who holds production access
- [ ] Play Console account and the signing key custody plan

**Design**
- [ ] Reconcile the mock fixture data (low-stock figures disagree across three screens)
- [ ] Dark-mode comps — a toggle ships in Settings and no dark screen exists
- [ ] Approve the stock-composition chart change (`prd.md` §3.3, `design.md` §5.10)
- [ ] Approve moving Reserved off blue

**Deliverable:** `docs/decisions/` committed, each decision with its rationale and date.

---

## Phase 1 — Foundation (2 weeks)

### Repository and tooling
- [ ] Multi-module Gradle project per `architecture.md` §3
- [ ] Version catalog; convention plugins for shared module config
- [ ] ktlint, Detekt (with the banned-pattern rules from `rules.md` §5.2), Konsist layer tests
- [ ] Pre-commit hooks; Conventional Commits; branch protection with one required approval

### Firebase
- [ ] Three projects: dev, staging, prod, with separate billing visibility
- [ ] Auth, Firestore, Storage, Functions, FCM, App Check, Crashlytics, Performance enabled in each
- [ ] Functions project: TypeScript, Zod, emulator suite, test harness
- [ ] `firestore.rules`, `storage.rules`, `firestore.indexes.json` in version control from commit one

### Core primitives
- [ ] `DataResult`, `UiText`, `Quantity`, `Money` value classes
- [ ] `FirebaseErrorMapper` with the full table from `rules.md` §6.1
- [ ] Dispatcher qualifiers, injectable `Clock`, logging wrapper
- [ ] Hilt graph; `core:common` utilities

### Design system *(from `design.md`)*
- [ ] Colour tokens, light and dark, including all semantic containers
- [ ] **Complete** type scale — all slots, none omitted. CaviTrack shipped nine of fifteen and it blocked a feature
- [ ] Spacing, radius, elevation, motion tokens
- [ ] `StockProTheme` with light/dark/system and persistence
- [ ] Paparazzi screenshot harness

### Auth, end to end
- [ ] Sign in, forgot password, sign out clearing all local data
- [ ] `onUserCreate` function assigning custom claims
- [ ] Claims read on the client; forced refresh on role change
- [ ] Plant resolution: none / one / several
- [ ] Session handling for shared devices

### CI/CD
- [ ] PR pipeline: lint → Detekt → Konsist → unit tests → coverage gate → emulator rules tests → assemble
- [ ] Main pipeline: deploy functions, rules, and indexes to dev; publish an internal build
- [ ] Crashlytics wired in all variants
- [ ] Release signing configured; key in secure custody

**Exit:** a user signs in on a real device against dev, theme switching works, CI is green, an internal build is installable.

---

## Phase 2 — The Write Path (3 weeks)

The most important phase. Everything after it is comparatively routine.

### Data model
- [ ] Firestore collections per `architecture.md` §4
- [ ] Room schema mirroring them, with FTS4 over name, code, and category
- [ ] Outbox table with state machine: pending → posting → done | failed
- [ ] Migration test harness from schema version 1
- [ ] Seed data: one org, two plants, ~1,250 items, six months of ledger history

### Server mutations
- [ ] `postMovement` — the core function: App Check → auth → claims → role check → idempotency check → transaction (read item, validate, append ledger, update projection, evaluate alert) → typed response
- [ ] `createItem`, `updateItem`, `deactivateItem`
- [ ] `adjustStock` with mandatory reason code and a variance threshold notification
- [ ] Shared validation module, mirroring `StockMath` in Kotlin
- [ ] Typed error envelope consumed by the client mapper

### Client write path
- [ ] `PostMovementUseCase`: validate → optimistic Room apply → enqueue
- [ ] `OutboxWorker`: FIFO per item, parallel across items, exponential backoff
- [ ] 2xx reconcile · 4xx rollback and surface · 5xx retain and retry
- [ ] "Needs attention" screen for failed mutations
- [ ] Sync-state model exposed app-wide

### Read sync
- [ ] Delta pull with per-collection cursors
- [ ] Bounded realtime listener on recently-changed items
- [ ] Full resync on login, plant switch, and on demand
- [ ] Plant switch clearing scoped local data, with the unsynced-changes guard

### Security
- [ ] Rules: client read-only, scoped by org and plant
- [ ] Emulator rules tests: one per role per collection per operation
- [ ] App Check enforced on all callables

### Tests
- [ ] `StockMath` at 100% branch coverage
- [ ] Ledger property test: any movement sequence leaves sum == projection
- [ ] Outbox: enqueue, drain, backoff, idempotent replay, rollback, ordering
- [ ] Function integration tests on the emulator

**Exit:** a movement posted offline on a real device appears correctly in Firestore after reconnect; posting it twice does not double-count; an unauthorised role is rejected **when called directly, without the app**.

---

## Phase 3 — Inventory & Movements (3 weeks)

First phase producing screens. Each is a vertical slice on Phase 2's machinery.

### Components *(design system)*
- [ ] Button — all variants × default/pressed/disabled/loading
- [ ] Text field — idle/focused/error/filled/disabled
- [ ] `StatusBadge`, `TrendIndicator` (with an explicit `higherIsBetter` flag), `IconTile`, `Avatar`, `SectionHeader`, `FilterChip`
- [ ] `ItemListRow`, `SettingsRow`, `ActivityRow`
- [ ] `EmptyState`, `ErrorState`, skeletons, `SyncBanner`
- [ ] `NumericStepper` — 56 dp targets, long-press acceleration, haptics

### Inventory
- [ ] List backed by Paging 3 over Room
- [ ] Local FTS search, debounced, under 100 ms
- [ ] Class tabs — **only the selected tab's data collected**
- [ ] Filter sheet and sort
- [ ] Status summary tiles
- [ ] Item detail with ledger history
- [ ] Add and edit item, role-gated
- [ ] Row overflow actions

### Movement flows
- [ ] Receive, issue (with availability check and reorder warning), adjust, production, transfer
- [ ] Shared item-picker with scan and search
- [ ] Optimistic UI throughout
- [ ] Insufficient-stock and permission-denied handling

### Quality
- [ ] All four states on every screen
- [ ] Screenshot tests: light/dark × scale 1.0/2.0
- [ ] Verified at 320 dp and 840 dp

**Exit:** a Store Keeper completes receive and issue on a real device, offline, with the changes syncing correctly.

---

## Phase 4 — Home, Reports & Scanning (3 weeks)

### Scheduled functions
- [ ] `dailySnapshot` per plant at 02:00 plant-local
- [ ] **`reconcileLedger`** — re-sum, compare, alert on drift, auto-heal via `ADJUSTMENT`
- [ ] `lowStockSweep`
- [ ] `monthlyRollup`
- [ ] Backfill snapshots for the seeded history

### Home
- [ ] KPI row with month-over-month deltas and semantic direction
- [ ] Inventory value with sparkline; **role-gated** privacy toggle
- [ ] Stock composition chart (revised per `design.md` §5.10)
- [ ] Quick actions, activity feed, top-5 low stock, upcoming reorders
- [ ] Notification badge

### Charts (Compose Canvas)
- [ ] Donut / composition with legend and semantic summary
- [ ] Area sparkline
- [ ] Multi-series line with markers and gridlines
- [ ] Entry animation, disabled under reduced motion

### Reports
- [ ] Date-range picker; four sections; period KPIs
- [ ] Data read from snapshots, never the raw ledger
- [ ] Low-stock table
- [ ] `generateReport` function → PDF and CSV → Storage → signed URL
- [ ] Sync-then-push for generations over ten seconds
- [ ] Android share sheet

### Scanning
- [ ] CameraX + ML Kit bundled model
- [ ] `ImageAnalysis` with `KEEP_ONLY_LATEST`; `imageProxy.close()` in a `finally`; formats limited to QR and Code-128; ~1.5 s duplicate debounce
- [ ] Analyzer executor shut down in `onDispose` alongside `unbindAll()`
- [ ] Permission rationale, torch, manual-entry fallback
- [ ] Resolution: local match · other plant · no match
- [ ] Entry points: Inventory header, receive form, issue form

**Exit:** reports render for an arbitrary range in under a second; scan to item detail under two seconds; the reconciliation job runs nightly and reports zero drift.

---

## Phase 5 — Settings, Team, Notifications & Tools (2–4 weeks)

Duration depends entirely on the Phase 0 tooling decision.

### Settings
- [ ] Profile view and edit with avatar upload
- [ ] Change password
- [ ] Appearance, language, unit system
- [ ] **Per-type notification preferences** — not one master toggle (`prd.md` §8.2)
- [ ] Sync status with last-synced time and pending count
- [ ] Data export
- [ ] Clear cache with size estimate
- [ ] Help, feedback, support
- [ ] About with real version and build

### Team
- [ ] Member list with roles
- [ ] Invite by email; claim assignment on acceptance
- [ ] Role change with forced token refresh
- [ ] Deactivate, with an explicit statement of what happens to that user's history

### Notifications
- [ ] FCM registration and token refresh
- [ ] Permission request with rationale (Android 13+)
- [ ] Triggers: low stock, out of stock, PO due, large adjustment, sync failure
- [ ] Deep links into the relevant entity
- [ ] Badge count synced with unread
- [ ] Notification centre

### Tools — **branch on Phase 0**

*If injection moulding (option a, ~4 weeks):*
- [ ] `toolingProfile`: cavity count, blocked cavities, rated shot life, cumulative shots, maintenance interval
- [ ] Shot counter incremented atomically by production movements
- [ ] Tool-health derivation and a toolroom dashboard sorted by urgency
- [ ] Maintenance logging with counter reset
- [ ] Cavity block / unblock
- [ ] PM-threshold push alerts

*If general tools (option b, ~1 week):*
- [ ] Issue and return with custody tracking
- [ ] Calibration due dates and alerts
- [ ] Tool status: available, issued, in calibration, retired

**Exit:** a Plant Manager invites a Store Keeper, who signs in with correctly restricted access and receives a low-stock push.

---

## Phase 6 — Hardening (2 weeks)

### Performance
- [ ] Baseline Profile generation
- [ ] Inventory list at 60 fps with 5,000 items
- [ ] Verify thumbnails, not originals, in lists
- [ ] Compose recomposition audit on list and dashboard
- [ ] Cold start under 2 s on a mid-range device
- [ ] Memory profiling across repeated scanner navigation
- [ ] APK under 25 MB

### Offline stress — structured, not casual airplane-mode checks

| Scenario | Expected |
|---|---|
| Full shift of movements offline, then reconnect | All post; none duplicated |
| Two devices move the same item while both offline | Both deltas apply; balance correct |
| App killed mid-post | No partial write; outbox intact |
| Queued issue exceeds stock on arrival | Rejected cleanly into "Needs attention" |
| 72 hours offline with continuous use | No unbounded local growth |
| Clock skew between devices | Server timestamps authoritative |
| Photo upload interrupted repeatedly | Backoff; no orphaned Storage objects |
| Token expiry while offline | Refresh on reconnect; queue survives |

### Accessibility audit
- [ ] TalkBack pass on every screen
- [ ] Contrast verified in both themes
- [ ] Font scale 2.0 with no clipping anywhere
- [ ] Every target ≥ 48 dp
- [ ] Confirm no meaning by colour alone — specifically the composition legend and the inverted low-stock delta
- [ ] Reduced motion honoured

### Security review
- [ ] Dependency CVE scan
- [ ] **Call every callable function directly, without the app, as every role** — the real test of ADR-002
- [ ] Attempt cross-org and cross-plant reads
- [ ] Confirm no secrets in the APK; verify App Check cannot be bypassed
- [ ] Confirm no PII in logs or Crashlytics
- [ ] Release-build regression — ProGuard deserialisation failures appear nowhere else

**Exit:** every scenario above passes; the security review finds no data exposed across tenant boundaries.

---

## Phase 7 — Production Readiness (2 weeks)

The phase that separates a working app from a system a company can depend on. None of it is visible in a screenshot and all of it is required.

### Infrastructure
- [ ] Production project locked down; access limited to two named people
- [ ] Daily Firestore export to Cloud Storage, 90-day retention
- [ ] **Restore rehearsal performed, timed, and documented** — an untested backup is not a backup
- [ ] Cross-region Storage replication
- [ ] Budget alerts and quota limits

### Monitoring
- [ ] Alerts per `architecture.md` §10.3, with **ledger drift paging immediately**
- [ ] Dashboards: function latency and errors, outbox depth, failed mutations, snapshot job health
- [ ] On-call rotation or, at minimum, a named owner with a stated response window

### Runbooks
- [ ] Restore from backup
- [ ] Investigate and resolve ledger drift
- [ ] Clear a stuck outbox on a user's device
- [ ] Roll back a bad function deploy
- [ ] Roll back a bad app release
- [ ] Emergency access revocation

### Governance
- [ ] Privacy policy published and linked in-app
- [ ] DPDP Act review of employee data handling
- [ ] Play Data Safety declaration
- [ ] Account deletion, in-app and via a documented web path
- [ ] Data-ownership and export commitment documented for the business
- [ ] Support path: how a user reports a problem and what happens next

### Launch
- [ ] Play Console listing, screenshots, and copy
- [ ] Internal testing track → closed testing with the pilot plant
- [ ] Staged rollout 5% → 25% → 50% → 100%, with 24 h Crashlytics observation at each step
- [ ] Training material for each role
- [ ] **Parallel-running plan** — the existing spreadsheet or paper process stays live and is reconciled daily during pilot

**Exit:** one plant runs a full week of real operations in the app, counts reconcile to ±2%, and the restore rehearsal is signed off.

---

## Timeline

| Phase | Duration | Cumulative |
|---|---|---|
| 0 — Decisions | 1 wk | 1 wk |
| 1 — Foundation | 2 wk | 3 wk |
| 2 — Write path | 3 wk | 6 wk |
| 3 — Inventory & movements | 3 wk | 9 wk |
| 4 — Home, reports, scanning | 3 wk | 12 wk |
| 5 — Settings, team, tools | 2–4 wk | 14–16 wk |
| 6 — Hardening | 2 wk | 16–18 wk |
| 7 — Production readiness | 2 wk | **18–20 wk** |

**Four to five months for two engineers. Seven to eight for one.**

Anyone quoting eight weeks for this is quoting Phases 3 and 4 and omitting the write path, hardening, and production readiness — which is precisely the set of work that determines whether a company can run its stock on it.

---

## Risk Register

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Phase 0 decisions drift unanswered | Blocks everything | High | Time-box to one week; escalate to a named decision-maker |
| Tooling scope turns out to be option (a) | +3 weeks | Medium | Decide in Phase 0; the model already reserves the field |
| iOS turns out to be contractual | Reopens ADR-001 | Medium | Ask now, not in month four |
| Offline sync edge cases surface late | Data integrity | Medium | Phase 2 builds it first; Phase 6 stress-tests it deliberately |
| Users bypass the app during pilot | Counts diverge, trust lost | High | Parallel running, daily reconciliation, role-specific training |
| Scope creep from "can it also do…" | Timeline | High | `prd.md` §4.2 is the V2 list; additions trade against it explicitly |
| Firestore aggregation outgrows snapshots | Reporting performance | Low | BigQuery export path already identified (ADR-005) |
| Single engineer is a bus factor of one | Project | Medium | Documentation, ADRs, and a second person with production access |

---

## Definition of Done

A phase is done when: code is merged and CI is green · tests meet the gate · all four states exist on every new screen · screenshot tests pass in both themes · accessibility verified at font scale 2.0 · no new Detekt or Konsist violations · documentation updated · demonstrated end to end on a real device, not an emulator.
