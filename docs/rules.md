# StockPro — Engineering Rules & AI Boundaries

**Version:** 1.0
**Applies to:** every contributor, human or AI-assisted
**Enforced by:** ktlint · Detekt · Konsist · CI gates · code review

---

## 1. The Seven Non-Negotiables

A pull request violating any of these is rejected without discussion.

1. **No placeholder code.** Every function has a complete, working body.
2. **No client writes a quantity.** All mutations go through a Cloud Function.
3. **No floating-point money or quantities.** `Long` minor units, always.
4. **Every mutation carries an idempotency key.**
5. **No raw exception text reaches a user.**
6. **No hardcoded user-facing strings.**
7. **Every async surface handles four states:** loading, content, empty, error.

---

## 2. Carried Forward From CaviTrack

CaviTrack was well built. These patterns worked and are adopted deliberately rather than rediscovered.

### 2.1 `DataResult<T>`

```kotlin
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Error(val message: UiText, val cause: Throwable? = null) : DataResult<Nothing>
}
```

Every fallible repository and use-case function returns it. No repository throws to its caller. `when` over it is exhaustive and never carries an `else` — an `else` hides the compile error you want when a variant is added.

**One change from CaviTrack:** `Error.message` is a `UiText`, not a `String`. CaviTrack passed raw strings through in places, which is how a Firebase console URL ended up on a user's screen.

### 2.2 `UiText`

```kotlin
sealed interface UiText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Dynamic(val value: String) : UiText
}
```

`Resource` is the default. `Dynamic` is for values that genuinely come from data — an item code, a bin location — never for English somebody did not want to add to `strings.xml`.

### 2.3 Cancellation protocol

```kotlin
try {
    // suspending work
} catch (e: CancellationException) {
    throw e                                    // structured concurrency depends on this
} catch (e: Exception) {
    DataResult.Error(errorMapper.map(e), e)
}
```

`catch (e: Exception)` swallows `CancellationException` because it *is* an `Exception`. Swallowing it means a cancelled scope keeps working after its screen is gone. Rethrow first, every time, with no exceptions anywhere in the codebase. Detekt enforces it.

### 2.4 ProGuard keeps for serialised models

CaviTrack got this right and most projects do not: release builds silently fail to deserialise when R8 strips model fields. Keep rules for every Firestore DTO and every `@Serializable` class, and **run the full regression suite against a release build**, because this class of failure appears nowhere else.

### 2.5 Also kept

App Check with Play Integrity · a real account-deletion flow · Paging configured with a sensible prefetch distance · debounced search · dispatcher injection via qualifiers · `SharingStarted.WhileSubscribed(5_000)` for hot flows.

---

## 3. Architecture Rules

### 3.1 A use case must earn its existence

CaviTrack had roughly seventeen classes shaped like this:

```kotlin
// This does nothing.
class GetComponentsUseCase @Inject constructor(private val repo: InventoryRepository) {
    operator fun invoke(q: String) = repo.getComponents(q)
}
```

A use case exists only when it does at least one of: orchestrates more than one repository; enforces a business rule; performs a non-trivial transformation; composes several flows. `PostMovementUseCase` (validate → permission → optimistic apply → enqueue) qualifies. `GetItemUseCase` does not — the ViewModel calls the repository.

### 3.2 Layer boundaries, enforced by test

```
feature:*  →  core:domain, core:designsystem, core:ui, core:common
core:data  →  core:domain, core:database, core:network
core:domain →  nothing  ★
```

`core:domain` is pure Kotlin. No `android.*`, no `androidx.*`, no `com.google.firebase.*`. A domain model that cannot be constructed in a plain JVM test is not a domain model. Konsist tests fail the build on violation.

Feature modules never import each other. Cross-feature navigation goes through route definitions in `app`.

### 3.3 Immutability

```kotlin
data class Item(
    val id: String,
    val code: String,
    val onHand: Quantity,
    val blockedBins: List<String> = emptyList(),   // List, never MutableList
)
```

`val` only. No `var` in a domain model. Expose read-only collection types. Mutate via `.copy()`. Derived values are computed properties or pure functions, not stored fields — except where a store is needed for a Firestore query, which must be documented at the field.

### 3.4 Enum parsing is always total

Firestore will eventually hand you an unknown string — a schema change, a console edit, a newer app version. Every enum carries `Unknown` and parses safely:

```kotlin
fun String?.toItemClass(): ItemClass =
    ItemClass.entries.firstOrNull { it.name == this } ?: ItemClass.Unknown
```

`valueOf()` on data from the network is banned.

### 3.5 State and events

Screen state is one `StateFlow<ScreenUiState>` of an immutable class. One-shot events are a `SharedFlow`. Composables take data and lambdas, never a ViewModel — a composable taking a ViewModel cannot be previewed or screenshot-tested.

---

## 4. Money and Quantity — The Precision Rules

This section prevents the most expensive class of bug in inventory software.

### 4.1 Never use floating point

```kotlin
// BANNED
val quantity: Double = 12.3
val price: Double = 450.50

// REQUIRED
@JvmInline value class Quantity(val milli: Long)    // 12.3 kg → 12_300
@JvmInline value class Money(val paise: Long)       // ₹450.50 → 45_050
```

`0.1 + 0.2 != 0.3` in IEEE 754. Ten thousand movements later, a warehouse's books do not balance and nobody can find why. Value classes cost nothing at runtime and make the unit part of the type, so `Quantity + Money` will not compile.

**Firestore stores numbers as 64-bit doubles**, which represent integers exactly up to 2^53. Long paise is safe to about ₹90 trillion. Fine.

### 4.2 Canonical storage, display conversion

Store the canonical unit. Convert only at the display boundary, driven by the user's unit preference. **Never persist a converted value** — that is how inventories silently drift. The Settings unit-system toggle affects rendering and input defaults only.

### 4.3 Formatting

```kotlin
// Currency — compact form follows locale: ₹1.24 Cr in en_IN, not ₹12.4M
NumberFormat.getCompactNumberInstance(locale, SHORT)

// Quantities — grouped, NEVER abbreviated.
// Someone reconciling against a physical count needs 1,250 kg, not 1.3k kg.
NumberFormat.getNumberInstance(locale)
```

Dates and relative times through `java.time` with locale-aware formatting and real plural rules. No string concatenation for "2 hours ago".

### 4.4 Time

`java.time` only. `java.util.Date` and `Calendar` are banned outright — a plant running three shifts across a DST boundary will find every `Calendar` bug left in the code. Store epoch millis; server timestamps are authoritative for ordering. Inject a `Clock`; `System.currentTimeMillis()` in business logic makes tests non-deterministic.

---

## 5. What to Avoid

### 5.1 Banned libraries

| Banned | Instead | Why |
|---|---|---|
| Any charting library (MPAndroidChart, Vico) | Compose `Canvas` | Four chart types. A dependency plus `View` interop costs more than writing them. |
| Retrofit / OkHttp | Firebase SDK + callable functions | There is no REST API to call |
| RxJava, LiveData | Coroutines and Flow | |
| Gson, Moshi | `kotlinx-serialization` | Already present for nav routes |
| Accompanist | Compose / Material 3 proper | Permissions, pager, and system UI all landed upstream |
| Timber | One logging wrapper gated on `BuildConfig.DEBUG` | |
| Any DI other than Hilt | | |
| Any image cropper | CameraX + a compression utility | Users photograph a bin label; they do not crop |
| Tokens in `SharedPreferences` | Firebase Auth's own secure storage | |

Every new dependency needs written justification: what the platform cannot do, size impact, maintenance status (a release within 12 months), licence. Single-maintainer packages with no release in 18 months are rejected by default.

### 5.2 Banned patterns

| Pattern | Why |
|---|---|
| `!!` | Handle the null. CaviTrack had zero instances; keep it at zero. |
| `GlobalScope` | Unscoped work outliving every lifecycle |
| `runBlocking` in production code | Blocks the caller's thread |
| `printStackTrace()` | Goes nowhere in release |
| `// TODO` in merged code | §7.1 |
| Business logic in a `@Composable` | Recomposition runs it an unpredictable number of times |
| `LaunchedEffect(Unit)` for anything that must re-run | Use a real key |
| `Context` held in a ViewModel field | Leaks the Activity |
| Collecting data for an off-screen tab | CaviTrack tripled its Firestore reads this way |
| Loading a full-resolution image in a list | The likeliest cause of jank on the Inventory screen. Request the thumbnail derivative. |
| Deriving stock level on the client | The server computes it. Two sources of truth guarantee divergence. |
| Unpaged `listAll()` on Storage | Silently under-deletes past ~1000 objects (CaviTrack defect 7) |
| `try { } catch (_) { }` | Silent failure; a frozen screen with no explanation |
| Hardcoded colours, spacing, radii, font sizes | Breaks theming and the grid |
| Re-fetching everything after one mutation | Room invalidates reactively; let it |

---

## 6. Errors and Edge States

### 6.1 The error mapper is mandatory

CaviTrack rendered `error.localizedMessage` directly, and when a composite index was missing, users saw a Firebase console URL. Every exception crossing the data boundary passes through one mapper.

| Condition | User sees | Logged |
|---|---|---|
| Network unavailable | "You're offline. Changes are saved and will sync." | info |
| `PERMISSION_DENIED` | "You don't have permission for this." | full + role + path |
| `FAILED_PRECONDITION` (missing index) | "Something went wrong loading this." | **full, to Crashlytics — this is a deployment bug** |
| `INSUFFICIENT_STOCK` | "Only 8 pcs available." | info |
| `ALREADY_EXISTS` (idempotent replay) | *nothing* — reconcile silently | debug |
| `NOT_FOUND` | "This item no longer exists." | info |
| `UNAUTHENTICATED` | Silent refresh; on failure, sign in with a return path | warn |
| Anything else | "Something went wrong. Please try again." | full |

The user never sees a stack trace, an SDK string, or an HTTP code. Engineering always sees everything.

### 6.2 Empty states name the cause

| Surface | Condition | Copy | Action |
|---|---|---|---|
| Inventory | No items | "No items yet. Add your first item to start tracking stock." | Add item |
| Inventory | Search miss | "No results for 'bearin'. Check the spelling or try the item code." | Clear search |
| Inventory | Filter miss | "No components match these filters." | Clear filters |
| Low stock | Nothing low | "Nothing needs reordering." | — |
| Activity | No movements | "No activity yet. Stock movements appear here." | Receive stock |
| Reports | No data in range | "No data for June 2024. Try a wider range." | Change range |
| Item history | No entries | "No movements recorded for this item." | — |
| Scanner | No match | "No item matches this code." | Add item · Search |
| Needs attention | Empty | "Everything synced." | — |

Empty is not error is not loading. An empty warehouse is not a bug, and rendering an error illustration for one teaches people to distrust real errors.

### 6.3 Loading

Skeletons, not spinners, for structural content — the layout is known, so skeletons eliminate shift.

| Surface | Treatment |
|---|---|
| Home KPI row | Four shimmer cards at final dimensions |
| Charts | Shimmer block at the chart's exact height |
| Inventory list | Six shimmer rows at 72 dp |
| Reports table | Real header, five shimmer rows |
| Settings | No skeleton; only values shimmer |
| Button submit | In-place spinner, **width held fixed** so the layout cannot shift under a finger mid-tap |

Shimmer period 1200 ms. Never faster than 800 ms — it reads as a glitch.

**Because Room is the source of truth, skeletons appear only on a genuine first load.** A returning user sees real data immediately while revalidation happens quietly.

### 6.4 Surfacing

| Kind | Surface |
|---|---|
| Transient success | Snackbar |
| Persistent condition (offline, pending) | Inline banner — a snackbar announcing a sync failure will be missed |
| Decision required | Dialog |
| Field validation | Inline `supportingText`, never a toast |
| Section failure | Error state scoped to that section only, with Retry |
| Destructive action | Always confirm |

Error scope matches failure scope. One failed chart darkens that card, not the screen.

### 6.5 Writing the copy

From the design guidance, and it applies to every string in the app:

- Errors explain what happened and how to fix it. They do not apologise and they are never vague.
- An empty screen is an invitation to act, not a mood.
- An action keeps its name through the whole flow: a button that says **Receive stock** produces a confirmation that says **Stock received**.
- Sentence case. Plain verbs. No filler.

---

## 7. AI Coding Boundaries

### 7.1 No placeholders

Banned in anything proposed for merge:

```kotlin
// TODO: implement later
// ... rest of the implementation
TODO("not yet implemented")
```

If a requirement is ambiguous, **ask before generating**. A plausible stub compiles, gets merged, and silently does nothing in production. That is worse than no code.

### 7.2 No invented dependencies, functions, or fields

- Every import resolves to something declared in `libs.versions.toml`.
- Every callable function name exists in `functions/src/`. Do not invent `bulkUpdateItems` because it was convenient.
- Every field exists in the schema in `architecture.md` §4.
- Material icon names are frequently hallucinated. Verify against `material-icons-extended`.
- Do not import `androidx.compose.material.*` — this is Material 3.
- If unsure whether a Firebase coroutine extension exists, use the `Task` API with `.await()`.

### 7.3 Firestore and Functions specifics that get generated wrong

- Reads before writes inside `runTransaction`. Always.
- Transaction bodies are **pure** — they retry, so no logging, no analytics, no emissions inside one.
- Every compound query ships its index in the same pull request.
- `FieldValue.increment()` is correct for fire-and-forget counters and **wrong** when the resulting value must be validated in the same operation — which, for stock, it always is.
- `whereIn` caps at 30 values. Chunk beyond that.
- Firestore has no cross-field `OR`. Do not generate one.
- `listAll()` is paginated. Follow the page token.

### 7.4 Responsiveness and accessibility are not optional

- No hardcoded pixel widths for layout. Breakpoints: compact < 600 dp, medium 600–840, expanded > 840.
- The Home KPI row scrolls horizontally on compact and becomes a 2×2 grid on medium.
- The Reports table scrolls inside its own container; the page body never scrolls sideways.
- **Never clamp font scale to protect a layout.** Fix the layout. Test at 0.85, 1.0, 1.3, 1.6, 2.0.
- All text uses theme styles. No inline `fontSize`.
- Minimum touch target 48 dp; 56 dp on Store Keeper paths.
- Every icon-only control has a content description.
- No meaning carried by colour alone.
- Charts expose a semantic summary: "Stock composition: 980 in stock, 18 low, 12 out of stock."
- Honour reduced-motion.

### 7.5 Scope discipline

Change what was asked. Do not reformat unrelated files, rename variables for style, or improve adjacent code in the same commit. Large incidental diffs make review impossible and bury the real change. Raise unrelated problems separately.

### 7.6 Read the neighbour first

| Writing | Read first |
|---|---|
| A repository | `core:data/repository/ItemRepositoryImpl` |
| A ViewModel | `feature:inventory/InventoryViewModel` |
| A screen | `feature:inventory/InventoryScreen` |
| A design-system component | `core:designsystem/component/StatusBadge` |
| A Cloud Function | `functions/src/mutations/postMovement.ts` |
| A test | `core:domain/StockMathTest` |

A file that looks different from its neighbours is wrong, even when it is individually better.

---

## 8. Testing

| Layer | Requirement |
|---|---|
| `StockMath` and all domain arithmetic | **100% branch coverage.** This decides quantities. Non-negotiable. |
| Use cases | Every branch, with fakes — not mocks of framework types |
| Repositories | Success, each failure class, cache hit, cache miss, offline |
| ViewModels | Every state transition, via Turbine |
| Room | Migration tests for every schema version. A failed migration destroys a user's queue. |
| **Outbox** | Enqueue · drain · backoff · idempotent replay · rollback on 4xx · ordering per item |
| Cloud Functions | Unit + Firebase emulator integration |
| **Security rules** | One emulator test per role per collection per operation |
| **Ledger integrity** | Property test: any sequence of movements leaves ledger sum == projection |
| Compose | Every screen renders all four states; screenshot tests via Paparazzi, light and dark, scale 1.0 and 2.0 |
| End to end | Sign in · receive · issue · adjust · scan · report · plant switch · full offline round trip |

**Gates:** overall ≥ 80%, `core:domain` ≥ 95%. Every bug fix ships the regression test that would have caught it — CaviTrack's two real defects were caught by none of its tests, and that is the gap being closed.

---

## 9. Review Checklist

- [ ] No placeholders, no `TODO`, no `!!`, no `GlobalScope`
- [ ] `CancellationException` rethrown before the generic catch
- [ ] No client-side quantity write
- [ ] Money and quantities are `Long` minor units
- [ ] Mutation carries an idempotency key
- [ ] Errors pass through the mapper; no raw text reaches the UI
- [ ] All strings externalised; formatting is locale-aware
- [ ] All four async states implemented
- [ ] No hardcoded colours, spacing, radii, or font sizes
- [ ] Compound query ships its index
- [ ] Touch targets ≥ 48 dp; verified at font scale 2.0
- [ ] Verified in light and dark
- [ ] No meaning by colour alone; icon-only controls labelled
- [ ] Only visible-tab data collected
- [ ] Domain module still imports nothing from Android or Firebase
- [ ] Tests included; gate met
- [ ] Release-build regression run if models or ProGuard changed
