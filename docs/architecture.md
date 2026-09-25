# StockPro — System Architecture

**Version:** 1.0
**Package:** `com.{company}.stockpro`
**Target:** production deployment inside a manufacturing company

---

## 1. Architecture Decision Records

These are the decisions that everything else follows from. Each names what was rejected and why, so they can be revisited deliberately rather than drifted away from.

### ADR-001 — Android-native (Kotlin + Compose), iOS deferred

**Decision:** Ship Android only for v1, built natively with Kotlin and Jetpack Compose. Structure the codebase so an iOS client can share the domain and data layers later via Kotlin Multiplatform.

**Rejected — Flutter:** one codebase for both platforms is the right general answer, and it was the recommendation before the CaviTrack context was known. It is the wrong answer here: the team has a working Compose codebase's worth of Android expertise, the users are on Android, and rewriting proven Firebase and Paging integration in a less familiar stack trades a known risk for an unknown one.

**Rejected — two native codebases now:** doubles cost for an unfunded iOS requirement.

**Consequence:** the `domain` module is **pure Kotlin with no Android dependencies**, enforced by an automated architecture test. That costs nothing today and makes KMP extraction a refactor rather than a rewrite. One approved screen was rendered on iOS; if iOS is contractual, that changes this ADR and the timeline in `phases.md`.

---

### ADR-002 — Server-authoritative writes; clients cannot write quantities

**Decision:** All mutations go through authenticated Cloud Functions. Firestore security rules grant clients **read-only** access to every collection. No client, ever, writes a stock quantity.

**Rejected — client-side Firestore transactions (CaviTrack's model):** it puts business rules in an artifact the company does not control. An old app version, a modified APK, or a bug posts a wrong quantity straight into the system of record. For a personal inventory tracker that is acceptable. For a company's stock ledger it is not.

**Consequences:**
- Validation, permission checks, ledger append, projection update, and alert evaluation all live in one server transaction. There is exactly one code path that can change a quantity.
- **Firestore's built-in offline write queue is not used.** That is the real cost of this decision. It is replaced by an explicit outbox in Room (ADR-004), which gives idempotency, ordering, retry policy, conflict surfacing, and a visible pending state — all things the built-in queue does not.
- Master-data writes (creating an item, editing a name) also go through functions, so there is one write path and one audit trail, not two.

---

### ADR-003 — Append-only ledger; the quantity is a projection

**Decision:** `stock_ledger` entries are immutable and append-only. `item.onHandMilli` is a cached projection maintained in the same transaction. A nightly job re-sums the ledger and alerts on any drift.

**Rejected — storing the quantity as the truth and overwriting it:** simpler, and it makes "why is this number wrong?" permanently unanswerable. An inventory system that cannot explain a number is not a system of record.

**Consequence:** corrections are new `ADJUSTMENT` entries, never edits. Reconciliation, audit reporting, and stock-take all become straightforward. Storage cost rises — roughly 200 bytes per movement, so a plant posting 500 movements a day accrues about 35 MB a year. Irrelevant.

---

### ADR-004 — Room as the local source of truth for reads

**Decision:** The UI reads only from Room. A sync service maintains Room from Firestore. Room also holds the outbox.

**Rejected — reading from Firestore's offline cache (CaviTrack's model, and its explicit "no Room" rule):** it cannot do substring search, which the approved design requires ("Search by name, code or category"). It cannot do the local joins the list needs. And with ADR-002 removing Firestore's write queue, its remaining advantage disappears.

**Consequences:**
- Search is a local FTS query: instant, offline, no Algolia or Typesense bill.
- Lists paint immediately from local data, then revalidate — no skeleton on a warm start.
- Reactive Room queries mean an edit invalidates every screen showing that item, which structurally prevents the stale-detail-screen defect from CaviTrack.
- Bounded by design: a plant's catalogue is thousands of items, not millions. If a plant ever exceeds ~50k items, revisit.

---

### ADR-005 — Firebase as the platform, with an exit path

**Decision:** Firebase Auth, Firestore, Cloud Functions, Cloud Storage, FCM, App Check, Crashlytics.

**Rejected — custom backend on Postgres:** better at the aggregation the Reports screen wants, and materially more to build and operate. Firebase gives auth, push, storage, hosting, and an ops story on day one, and the reporting workload is bounded by precomputed snapshots (§6).

**Consequence and the honest caveat:** Firestore is weak at ad-hoc aggregation. This is handled by precomputing daily and monthly rollups, so reports read tens of documents rather than scanning the ledger. If reporting requirements outgrow that — cross-plant analytics, arbitrary pivots, BI tooling — the answer is a nightly export into BigQuery, not a rewrite. Because all writes already pass through functions (ADR-002), the write path is portable: replacing Firestore later touches the repository implementations and the functions, not the app.

**Data ownership:** scheduled Firestore exports to Cloud Storage, plus an on-demand full export in the app. The company's data is never hostage to the platform.

---

## 2. Stack

> Pin every version in `gradle/libs.versions.toml`, resolved to current stable at project start. Do not copy version strings from this document.

### 2.1 Android client

| Concern | Choice | Note |
|---|---|---|
| Language | Kotlin 2.x | |
| UI | Jetpack Compose + Material 3 | |
| Build | Gradle Kotlin DSL + version catalog | CaviTrack declared dependencies inline; do not repeat that |
| Min / target SDK | 26 / current | |
| DI | Hilt | |
| Async | Coroutines, `StateFlow`, `SharedFlow` | |
| Navigation | Navigation Compose, type-safe routes via `kotlinx-serialization` | |
| Local DB | **Room** + FTS4 | ADR-004 |
| Paging | Paging 3 over Room | Not over Firestore — the mirror is local |
| Preferences | DataStore Proto | Typed, unlike the Preferences variant |
| Images | Coil | Thumbnail derivatives only in lists |
| Barcode | ML Kit Barcode Scanning, **bundled model** | Shop devices are often Play-services-stale |
| Camera | CameraX | |
| Background | WorkManager | Outbox drain, sync, upload |
| Charts | Compose Canvas, hand-built | Four chart types; a charting dependency costs more than it saves |
| Serialization | `kotlinx-serialization` | |
| Testing | JUnit5, Turbine, MockK, Robolectric, Compose UI test, Paparazzi (screenshot) | |
| Quality | ktlint, Detekt, Konsist (layer rules) | |

### 2.2 Backend

| Concern | Choice |
|---|---|
| Functions runtime | Node.js LTS + TypeScript, Firebase Functions v2 |
| Validation | Zod at every function boundary |
| Database | Cloud Firestore (Native mode) |
| Auth | Firebase Auth with custom claims (`orgId`, `plantRoles`) |
| Storage | Cloud Storage + an image-derivative function |
| Push | FCM, role-topic and token fan-out |
| Scheduled work | Cloud Scheduler → Pub/Sub → Functions |
| Reports | Server-side PDF (Puppeteer) and CSV, written to Storage, delivered as signed URLs |
| Integrity | App Check (Play Integrity) |
| Observability | Cloud Logging + Error Reporting + custom metrics; Crashlytics and Performance on the client |
| Analytics warehouse | Firestore → BigQuery export (deferred until needed) |

### 2.3 Environments

Three **separate Firebase projects**: `stockpro-dev`, `stockpro-staging`, `stockpro-prod`. Not three collections in one project. Separate projects mean a bad function deploy cannot touch production data, and production access can be restricted to two named people.

---

## 3. Module Structure

Multi-module from the start. This is a reversal from CaviTrack's single module, and it is justified here by build time, enforced layering, and KMP-readiness (ADR-001).

```
stockpro/
├── app/                          Application, MainActivity, nav host, DI wiring
│
├── core/
│   ├── domain/                   ★ PURE KOTLIN — no Android, no Firebase
│   │   ├── model/                Item, LedgerEntry, Plant, Role, Money, Quantity
│   │   ├── repository/           interfaces only
│   │   ├── usecase/              only where real logic exists (rules.md §3.1)
│   │   └── util/                 DataResult, StockMath, quantity arithmetic
│   ├── data/                     repository implementations, sync, outbox, mappers
│   ├── database/                 Room entities, DAOs, FTS, migrations
│   ├── network/                  Firebase clients, callable wrappers, error mapping
│   ├── designsystem/             tokens, theme, atoms, molecules, charts
│   ├── ui/                       shared Compose utilities, state holders, previews
│   ├── common/                   UiText, dispatchers, clock, logging, result helpers
│   └── testing/                  fakes, fixtures, test rules
│
├── feature/
│   ├── auth/                     sign-in, forgot password, plant selection
│   ├── home/                     dashboard
│   ├── inventory/                list, search, filter, detail, add/edit
│   ├── movements/                receive, issue, adjust, produce, transfer
│   ├── scanner/                  camera + ML Kit + payload resolution
│   ├── reports/                  sections, charts, generation, export
│   ├── settings/                 preferences, profile, team, sync status
│   └── notifications/            centre and preferences
│
├── functions/                    TypeScript Cloud Functions
│   ├── src/
│   │   ├── mutations/            postMovement, createItem, updateItem, adjustStock
│   │   ├── scheduled/            dailySnapshot, reconcileLedger, lowStockSweep, cleanup
│   │   ├── triggers/             onUserCreate, onImageUpload, onLedgerWrite
│   │   ├── reports/              generators
│   │   ├── domain/               shared validation and stock math, mirrored from Kotlin
│   │   └── lib/                  auth guards, idempotency, error envelope
│   └── test/
│
├── firestore.rules
├── firestore.indexes.json        ★ in version control, deployed by CI
├── storage.rules
└── docs/                         these five documents + ADRs
```

**Enforced by Konsist tests in CI:**

- `core:domain` imports nothing from `android.*`, `androidx.*`, or `com.google.firebase.*`.
- `feature:*` modules never import each other.
- Only `core:data` imports `core:network` and `core:database`.
- No ViewModel imports Firebase.

The star on `core:domain` matters: it is the module that makes ADR-001's iOS path cheap, and it stays pure only if a test enforces it.

---

## 4. Data Model

### 4.1 Firestore layout

```
organizations/{orgId}
├── (doc)                         name, plan, createdAt, settings
├── members/{uid}                 displayName, email, status, plantRoles{}, createdAt
├── auditEvents/{eventId}         non-quantity actions (immutable)
└── plants/{plantId}
    ├── (doc)                     code, name, timezone, currency, address
    ├── locations/{locationId}    code "A1-01", zone, rack, bin
    ├── categories/{categoryId}   name, itemClass
    ├── items/{itemId}
    ├── ledger/{entryId}          immutable, append-only
    ├── purchaseOrders/{poId}
    ├── snapshots/{YYYY-MM-DD}    daily rollup
    ├── monthlyRollups/{YYYY-MM}
    └── notifications/{notifId}
```

Scoping by `orgId` then `plantId` means every query is naturally bounded and every security rule is a prefix check.

### 4.2 Item document

```
items/{itemId}
  code                 "BRG-001"          unique per plant
  codeLower            "brg-001"          for case-insensitive lookup
  name                 "Ball Bearing"
  nameLower            "ball bearing"
  itemClass            COMPONENT | RAW_MATERIAL | FINISHED_GOOD | TOOL
  categoryId, categoryName                categoryName denormalised for list rendering
  unitOfMeasure        PCS | KG | L | M
  onHandMilli          320000             ← projection of the ledger; server-only
  reservedMilli        0
  inProductionMilli    0
  reorderLevelMilli    50000
  parLevelMilli        500000             ← the "500 pcs" figure; see prd.md §8.1
  unitCostPaise        45000              ← ₹450.00, integer minor units
  locationId, locationCode                code denormalised
  barcode
  imagePath                               Storage path; derivatives resolved client-side
  toolingProfile       { ... } | null     ← reserved for prd.md §7
  isActive             true
  createdAt, updatedAt, createdBy, updatedBy
  lastMovementAt                          drives "recently moved" sorting
```

**Every quantity is a `Long` in milli-units. Every money value is a `Long` in paise.** Never a floating-point number. See `rules.md` §4 — this is the single most common way inventory systems accumulate silent error.

### 4.3 Ledger entry

```
ledger/{entryId}
  itemId, itemCode, itemName              denormalised: correct as at the time
  type                 RECEIPT | ISSUE | PRODUCTION | ADJUSTMENT
                       | TRANSFER_IN | TRANSFER_OUT | SCRAP
  deltaMilli           +500000            signed
  balanceAfterMilli    820000
  unitCostPaise                           captured at movement time for valuation
  reference            "PO-2024-0142"
  reasonCode                              required for ADJUSTMENT and SCRAP
  note, attachmentPath
  fromLocationId, toLocationId
  actorUid, actorName                     denormalised
  idempotencyKey       UUID               unique index; prevents double-posting
  clientCreatedAt                         when the user acted (may be offline)
  createdAt                               server timestamp — authoritative
```

`clientCreatedAt` and `createdAt` are both kept deliberately. A movement made offline at 09:00 and posted at 14:00 is *reported* at 09:00 and *ordered* by 14:00. Collapsing them loses one or the other.

### 4.4 Daily snapshot

```
snapshots/{2026-06-15}
  itemCount, inStockCount, lowStockCount, outOfStockCount
  reservedMilliTotal, inProductionMilliTotal
  totalValuePaise
  movementsByType      { RECEIPT: 42, ISSUE: 87, ... }
  quantityByType       { RECEIPT: 12400000, ... }
  valueReceivedPaise, valueIssuedPaise
  computedAt
```

Written nightly. **Every report reads these.** A thirty-day range is thirty document reads; a six-month trend reads six monthly rollups. This is what makes Firestore viable for the Reports screen (ADR-005).

### 4.5 Indexes

`firestore.indexes.json` lives in version control and is deployed by CI. CaviTrack discovered a missing index in production and showed users the raw Firebase console URL in the error message. That cannot happen if the index ships with the query that needs it.

Required at minimum:
```
items:  (isActive, itemClass, nameLower)
items:  (isActive, itemClass, onHandMilli)
items:  (isActive, lastMovementAt desc)
ledger: (itemId, createdAt desc)
ledger: (type, createdAt desc)
ledger: (actorUid, createdAt desc)
```

**Rule: a pull request adding a compound query and not adding its index is incomplete.**

---

## 5. Security Model

### 5.1 Identity and claims

Firebase Auth custom claims, set by a Cloud Function on invite acceptance and on any role change:

```json
{
  "orgId": "org_7f3a",
  "plantRoles": { "plant_01": "INVENTORY_MANAGER", "plant_02": "STORE_KEEPER" }
}
```

Claims are read from the token, so authorisation requires no membership lookup per request. A role change forces a token refresh on the client; until it propagates, the server check is still authoritative.

### 5.2 Firestore rules — read-only for clients

```
match /organizations/{orgId} {
  allow read: if belongsToOrg(orgId);
  allow write: if false;                        // functions only

  match /plants/{plantId}/{document=**} {
    allow read: if hasAnyRoleInPlant(orgId, plantId);
    allow write: if false;                      // functions only
  }
}
```

That is the entire write policy. Every mutation is a callable function running with admin privileges after its own permission check. Rules become a read-authorisation layer, which is a much smaller thing to get right — and rules are tested against the emulator in CI, one test per role per collection.

Field-level read restrictions (Store Keepers must not see `unitCostPaise`) are handled by serving those fields through a function rather than by client-side hiding, because hiding a field in the UI does not hide it in the document.

### 5.3 Permission matrix

| Action | Store Keeper | Production Supervisor | QA | Inventory Manager | Plant Manager | Org Admin |
|---|---|---|---|---|---|---|
| Read catalogue | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Read valuation | — | — | — | ✓ | ✓ | ✓ |
| Receive | ✓ | — | — | ✓ | ✓ | ✓ |
| Issue | ✓ | ✓ | — | ✓ | ✓ | ✓ |
| Production | — | ✓ | — | ✓ | ✓ | ✓ |
| Scrap | — | ✓ | ✓ | ✓ | ✓ | ✓ |
| Transfer | ✓ | — | — | ✓ | ✓ | ✓ |
| **Adjust** | — | — | — | ✓ | ✓ | ✓ |
| Create / edit item | — | — | — | ✓ | ✓ | ✓ |
| Deactivate item | — | — | — | — | ✓ | ✓ |
| Manage team | — | — | — | — | ✓ | ✓ |
| Export data | — | — | — | ✓ | ✓ | ✓ |
| Manage plants | — | — | — | — | — | ✓ |

Enforced in the function, tested in the emulator, and **verified by calling the API directly without the app** as part of the Phase 6 security review.

### 5.4 Other controls

App Check with Play Integrity on all callable functions. Rate limiting per user. Storage rules restricting reads to org members and writes to functions. No PII in logs. Crashlytics scrubbed. TLS enforced; certificate pinning on the API host.

---

## 6. Write Path

The single most important flow in the system.

```
User taps "Receive 500 kg"
  │
  ├─ 1  Validate locally (fast feedback): quantity > 0, permission, item exists
  ├─ 2  Generate idempotencyKey = UUID
  ├─ 3  Write to Room in ONE transaction:
  │        outbox  ← pending mutation
  │        items   ← optimistic onHandMilli += 500000
  │        ledger  ← provisional entry, flagged pending
  ├─ 4  UI updates instantly from reactive Room query
  └─ 5  Enqueue OutboxWorker (expedited when online)

OutboxWorker
  │
  ├─ Call postMovement(payload, idempotencyKey)
  │
  ├── Function:
  │     a  verify App Check + auth
  │     b  resolve claims → check role for this plant + movement type
  │     c  check idempotencyKey — already seen? return the original result
  │     d  runTransaction:
  │          read item
  │          validate: issue ≤ onHand − reserved; adjustment has a reason
  │          append ledger entry with balanceAfter
  │          update item.onHandMilli, lastMovementAt
  │          if crossing reorderLevel → queue a low-stock notification
  │        commit
  │     e  return { entryId, balanceAfterMilli, stockLevel }
  │
  ├─ 2xx → replace the provisional entry with the server entry,
  │         set onHandMilli to the authoritative balance, clear pending
  ├─ 4xx → ROLL BACK the optimistic change, move to "Needs attention"
  │         with the server's reason (insufficient stock, no permission)
  └─ 5xx / network → leave pending, exponential backoff, banner shows the count
```

**Why the idempotency key is not optional:** a request that times out after the server committed is indistinguishable, from the client, from one that never arrived. Without the key, retrying double-receives 500 kg. With it, the function returns the original result and the client reconciles. This is the difference between an inventory system and a random number generator.

**Ordering:** the outbox drains FIFO per item. Two movements on the same item must not post out of order, because `balanceAfter` would be wrong. Movements on different items post in parallel.

---

## 7. Sync and Offline

### 7.1 Read sync

```
Firestore ──(delta pull: where updatedAt > cursor, paged)──► Room ──► UI
```

- **On app start and on foreground:** pull deltas for the active plant. Typically a handful of documents.
- **Realtime listener:** only on the active plant's items changed in the last hour, so a colleague's receipt appears without a manual refresh. Bounded, so it cannot become an expensive listener.
- **Full resync:** on first login, on plant switch, and on demand from Settings.
- **Cursor** stored per collection per plant in DataStore.

Costed: a plant with 1,250 items does one initial sync of ~1,250 reads, then tens of reads per foreground. Ten users on that plant cost a few thousand reads a day, well inside a negligible bill.

### 7.2 Offline behaviour

| Operation | Offline |
|---|---|
| Browse, search, filter, sort | Fully functional from Room |
| Item detail and history | Fully functional |
| Reports | Last-synced snapshots, banner names the date |
| Receive / issue / adjust / produce | Queued; optimistic; posts on reconnect |
| Scan | Fully functional — resolution is a local lookup |
| Create or edit item | Queued |
| Photo upload | Queued via WorkManager; the item shows a local URI meanwhile |
| Export and report generation | Requires connectivity; states so plainly |

**Sync state is always visible:** `Synced · 2m ago` / `Syncing…` / `3 pending` / `1 needs attention`. The Settings row already reserves a place for it.

### 7.3 Conflicts

| Situation | Resolution |
|---|---|
| Two people post movements on one item | Both apply. Deltas commute; the server serialises them. **This is why the ledger model matters** — with overwritten quantities one write would be lost. |
| Queued issue exceeds available stock on arrival | Server rejects `INSUFFICIENT_STOCK`; the mutation surfaces in "Needs attention" with the current figure, and the user re-enters or cancels. Never silently dropped. |
| Item edited on two devices | Field-level last-write-wins with `updatedAt`; large divergence shows a diff |
| Item deactivated server-side, movement queued locally | Server rejects; the user is told why |
| Same mutation posted twice | Idempotency key returns the original result |

---

## 8. Reporting

```
Cloud Scheduler (02:00 plant-local)
  → dailySnapshot(plantId)          aggregate the day, write snapshots/{date}
  → reconcileLedger(plantId)        re-sum ledger per item, compare to projection
                                     drift → alert + auto-heal via ADJUSTMENT
  → lowStockSweep(plantId)          catch anything the write path missed
  → monthlyRollup                   on the 1st
```

Report queries read snapshots, never the raw ledger. A date range of arbitrary length costs one document read per day in range.

`reconcileLedger` is what makes the ledger claim real rather than decorative. It runs every night, and a drift alert is a page-worthy event — it means something wrote a quantity outside the sanctioned path.

Generation: `generateReport(type, from, to, format)` → Storage → signed URL. Under ten seconds it returns directly; over ten it completes in the background and pushes when ready.

---

## 9. Presentation Layer

Unidirectional, one state object per screen:

```
Composable ──intent──► ViewModel ──► UseCase ──► Repository ──► Room / Function
     ▲                                                              │
     └──────────── StateFlow<ScreenUiState> ◄───── reactive Room ◄───┘
```

- Screen state is a `StateFlow` of a single immutable data class. Not seven parallel flows.
- One-shot events (navigate, snackbar, scan result) are a `SharedFlow`. Putting navigation in `StateFlow` refires it on rotation.
- Composables take data and lambdas. A composable that takes a ViewModel cannot be previewed or screenshot-tested.
- `collectAsStateWithLifecycle` everywhere.
- **Only the visible tab's data is collected** — CaviTrack collected all three tabs' paging flows at once, tripling reads. Structurally prevented here because the collection is keyed on the selected tab.
- Because Room is the source of truth and its queries are reactive, an edit invalidates every observer automatically. CaviTrack's stale-detail-screen bug cannot occur.

---

## 10. Operations

### 10.1 CI/CD

| Trigger | Pipeline |
|---|---|
| Pull request | ktlint · Detekt · Konsist layer tests · unit tests · coverage gate · emulator rules tests · assemble debug · screenshot-test diff |
| Merge to `main` | Above, plus deploy functions, rules, and indexes to **dev**; publish an internal build |
| Tag `v*` | Deploy to **staging** → smoke suite → manual approval → deploy to **prod** → Play staged rollout |

Rules and indexes deploy **with** the app version that needs them, never separately.

### 10.2 Backup and recovery

| Control | Detail |
|---|---|
| Firestore export | Scheduled daily to a Cloud Storage bucket with 90-day retention |
| Storage backup | Cross-region bucket replication |
| **Restore rehearsal** | Quarterly, into a scratch project, timed and documented |
| RPO / RTO | 1 hour / 4 hours |
| Ledger immutability | Rules deny update and delete on ledger documents; no code path edits one |
| Runbook | Documented procedures for: restore, ledger drift, stuck outbox, function outage, bad deploy rollback |

An untested backup is not a backup. The quarterly rehearsal is a hard requirement, not a nice-to-have.

### 10.3 Monitoring

| Signal | Alert |
|---|---|
| **Ledger drift** | Any occurrence — page immediately |
| Function error rate | > 1% over 5 min |
| `postMovement` p95 latency | > 2 s |
| Outbox items pending > 1 h (aggregate) | > 10 |
| Failed mutations in "Needs attention" | > 5 in an hour |
| Crash-free sessions | < 99.5% |
| Auth failure spike | 3× baseline |
| Daily snapshot job | Did not complete |
| Firestore cost | > 130% of the seven-day average |

### 10.4 Release

Semantic versioning. Staged Play rollout: 5% → 25% → 50% → 100%, with 24 hours of Crashlytics observation at each step. A remote-config kill switch for new features. Minimum supported version enforced server-side, so a client too old to satisfy current validation is told to update rather than allowed to post.

### 10.5 Compliance

| Requirement | Handling |
|---|---|
| DPDP Act 2023 (India) | Employee personal data limited to name, email, avatar. Documented purpose, retention, and deletion. |
| Play Data Safety | Declares camera, photos, account data, diagnostics |
| Account deletion | In-app request plus a documented web path (Play requirement) |
| Data export | Full org export on demand, documented format |
| Audit retention | Ledger and audit events retained seven years |
| Access review | Quarterly review of who holds production access |

### 10.6 Cost estimate

For one plant, 1,250 items, 10 users, 500 movements a day:

| Service | Monthly |
|---|---|
| Firestore reads (~400k) | ~₹20 |
| Firestore writes (~40k) | ~₹15 |
| Storage (5 GB + transfer) | ~₹50 |
| Functions (~50k invocations) | ~₹40 |
| FCM | free |
| **Total** | **well under ₹500/month per plant** |

Firebase cost is not a concern at this scale. Engineering time is the cost.
