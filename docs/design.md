# StockPro — Design System

**Version:** 1.0
**Implements:** `core:designsystem`
**Source:** four approved screens, reverse-engineered

> **Accuracy.** Light values are sampled from the approved screens and accurate to roughly ±2 per channel; treat them as the intended token, not a measurement. **Dark values are derived, not designed** — Settings ships a Light/Dark toggle and no dark screen was provided. Dark comps are a Phase 0 deliverable, and everything in §2.4 is a starting proposal to be reviewed against them.

---

## 1. Design Language

The approved screens establish a **soft-tinted card** system: white cards on a cool grey canvas, with each functional group given a pastel container and a matching saturated icon. Three traits define it:

1. **Tint pairs.** Every accent appears as a light container plus a saturated foreground. This is the most-repeated pattern in the entire UI and it is what makes the system feel coherent.
2. **Generous radii**, differentiated by hierarchy rather than applied uniformly.
3. **Near-flat elevation.** Cards separate from the canvas through a soft wide shadow, not a hard drop shadow.

**Where discipline is needed.** The brief's direction is followed exactly — this is the client's approved look. But two habits in the mocks should not be carried into the build: the same radius and the same shadow applied to every surface regardless of importance, which flattens hierarchy; and colour doing two unrelated jobs at once (§2.5). Both are corrected below.

---

## 2. Colour

### 2.1 Brand

| Token | Hex | Sampled from |
|---|---|---|
| `primary` | `#2563EB` | Add Item button, active tabs, FAB, active switch |
| `primaryPressed` | `#1E40AF` | derived |
| `primaryContainer` | `#EFF6FF` | blue tinted cards |
| `onPrimary` | `#FFFFFF` | |
| `brandMark` | `#065F46` | logo, "Pro" wordmark, Settings avatar |
| `accent` | `#10B981` | active nav icon and label |
| `accentContainer` | `#D1FAE5` | active nav pill, Home avatar |

> The dark green mark and the blue primary are two brand colours doing brand work. Confirm which is canonical — the current split reads as two products sharing a screen.

### 2.2 Semantic

| Token | Hex | Container | On-container | Meaning |
|---|---|---|---|---|
| `success` | `#16A34A` | `#D1FAE5` | `#065F46` | In stock, positive trend |
| `warning` | `#F59E0B` | `#FEF3C7` | `#92400E` | Low stock, attention |
| `warningStrong` | `#F97316` | `#FFF7ED` | — | Issue action, alert icons |
| `danger` | `#EF4444` | `#FEE2E2` | `#991B1B` | Out of stock, destructive |
| `dangerStrong` | `#DC2626` | `#FEF2F2` | — | Logout, critical |
| `reserved` | **`#0891B2`** | `#CFFAFE` | `#155E75` | **Changed from blue — see §2.5** |
| `production` | `#8B5CF6` | `#EDE9FE` | `#5B21B6` | In production, analytics |

### 2.3 Neutrals — light

| Token | Hex | Use |
|---|---|---|
| `canvas` | `#F5F7FA` | Screen background |
| `surface` | `#FFFFFF` | Cards, rows, app bar, inputs |
| `surfaceAlt` | `#F9FAFB` | Table headers, thumbnail placeholders |
| `surfaceNav` | `#F7F4F9` | Bottom nav — reads faintly lavender in the Android captures; **confirm this is intentional and not a capture artifact** |
| `outline` | `#E5E7EB` | Card borders |
| `outlineSubtle` | `#F1F3F5` | Row separators |
| `textPrimary` | `#0F172A` | Titles, values, item names |
| `textSecondary` | `#6B7280` | Subtitles, codes, labels |
| `textTertiary` | `#9CA3AF` | Placeholders, axis labels |
| `textDisabled` | `#C4C9D2` | |

### 2.4 Neutrals — dark *(derived, pending comps)*

| Token | Hex | Note |
|---|---|---|
| `canvas` | `#0F1215` | Not pure black — avoids OLED smear on fast lists |
| `surface` | `#1A1D21` | |
| `surfaceAlt` | `#22262B` | |
| `surfaceNav` | `#16191D` | |
| `outline` | `#2E333A` | |
| `textPrimary` | `#E8EAED` | Not pure white — reduces halation |
| `textSecondary` | `#9BA1AA` | |
| `textTertiary` | `#6B7280` | |

Accents lighten, containers darken and invert:

| Token | Light | Dark |
|---|---|---|
| `primary` | `#2563EB` | `#60A5FA` |
| `primaryContainer` | `#EFF6FF` | `#172554` |
| `success` / container / on | `#16A34A` / `#D1FAE5` / `#065F46` | `#4ADE80` / `#14532D` / `#BBF7D0` |
| `warning` / container / on | `#F59E0B` / `#FEF3C7` / `#92400E` | `#FBBF24` / `#78350F` / `#FDE68A` |
| `danger` / container / on | `#EF4444` / `#FEE2E2` / `#991B1B` | `#F87171` / `#7F1D1D` / `#FECACA` |
| `reserved` | `#0891B2` | `#22D3EE` |
| `production` | `#8B5CF6` | `#C4B5FD` |

### 2.5 Two colour problems, resolved

**Blue meant two things.** `#2563EB` was simultaneously the action colour (Add Item, FAB, active tab) and the Reserved status. A user cannot tell whether blue means "you can act on this" or "this is spoken for." **Reserved moves to teal `#0891B2`.** Blue now means exclusively: you can act here.

**Status was carried by colour alone in the legend.** The composition legend tied a coloured dot to a slice with no other channel, making In Stock and Out of Stock indistinguishable for a red/green-deficient user — roughly 8% of men, and this user base skews male. **Every status now carries colour + text**, and chart segments carry direct labels. The status badge already does this correctly; the fix is on the chart.

### 2.6 Dynamic colour is off

Material You would reassign these hues from the device wallpaper. Amber must mean "needs attention" on every device in the plant. `dynamicColor = false`, permanently, and the reason belongs in a comment so nobody re-enables it as a modernisation.

---

## 3. Typography

### 3.1 Family

**Roboto** — the platform default and what the approved Android screens render in. The screens show no typographic personality that would justify shipping a custom family, and system fonts scale correctly with accessibility settings for free.

**Tabular figures are mandatory** on every quantity, KPI value, counter, and table column: `FontFeature("tnum")`. Without it, digits jitter as values update and the Reports table columns will not align — visible in the approved capture.

### 3.2 Scale

Complete. CaviTrack defined nine of fifteen slots, `bodySmall` silently fell back to library defaults, and a missing `displayLarge` blocked a feature. Every slot is defined here.

| Token | Size | Line | Weight | Tracking | Use |
|---|---|---|---|---|---|
| `displayLarge` | 40 | 48 | Bold | −0.5 | Large counters, item detail quantity |
| `displayMedium` | 34 | 40 | Bold | −0.4 | Screen titles: Inventory, Reports, Settings |
| `displaySmall` | 28 | 34 | Bold | −0.3 | "Good Morning, Aditya" |
| `headlineLarge` | 24 | 30 | Bold | −0.2 | KPI values: 1,248 · ₹12.4M |
| `headlineMedium` | 22 | 28 | SemiBold | −0.1 | |
| `headlineSmall` | 20 | 26 | SemiBold | −0.1 | Card titles: Inventory Value, Stock Status |
| `titleLarge` | 18 | 24 | SemiBold | 0 | Section titles |
| `titleMedium` | 16 | 24 | SemiBold | 0 | Item names, settings row titles |
| `titleSmall` | 14 | 20 | Medium | 0 | Dense row titles |
| `bodyLarge` | 16 | 24 | Regular | 0 | Body, form values, quantities |
| `bodyMedium` | 14 | 20 | Regular | 0 | Subtitles, codes, table cells |
| `bodySmall` | 12 | 16 | Regular | 0.2 | Timestamps, chart axes, helper text |
| `labelLarge` | 14 | 20 | Medium | 0 | Buttons, tabs |
| `labelMedium` | 12 | 16 | Medium | 0.2 | Badges, nav labels |
| `labelSmall` | 11 | 16 | Medium | 0.4 | Dense metadata |

### 3.3 Two specialised styles

**Item and bin codes** — monospace, because `BRG-001` vs `BRG-00l` vs `BRG-0Ol` is a real confusion in a proportional face, and picking the wrong item misattributes a movement:

```
codeStyle = FontFamily.Monospace, 13sp, Medium, tracking 0.5sp
```

**Large quantity readout** — `displayLarge` with tabular figures and no count-up animation. Animating a number someone is reading against a physical count creates a window where the screen is lying.

### 3.4 Section headers stay sentence case

The approved screens render "Account Settings", "App Preferences", "Data & Backup" in title case, not uppercase. Keep it. Tracked-out all-caps eyebrow labels are a default reach that adds nothing here and reads worse at large font scales.

---

## 4. Tokens

### 4.1 Spacing — 4 dp base

`x1 4` · `x2 8` · `x3 12` · **`x4 16` (default)** · `x5 20` · `x6 24` · `x8 32` · `x10 40` · `x12 48`

| Constant | Value |
|---|---|
| Screen gutter | 16 |
| Card padding | 16 |
| Gap between cards | 12 |
| Gap between sections | 24 |
| List row min height | **72** |
| Bottom nav height | 64 + safe area |
| App bar height | 56 |
| Scroll padding above nav | 88 |

### 4.2 Radius — differentiated by hierarchy

Not one radius everywhere. Smaller elements take smaller radii, which is what makes nesting read correctly.

| Token | Value | Applied to |
|---|---|---|
| `xs` | 4 | Chart elements, progress bars |
| `sm` | 8 | Small icon tiles, chips |
| `md` | 12 | Buttons, inputs, search bar, thumbnails |
| `lg` | 16 | Cards, list rows, KPI tiles |
| `xl` | 20 | Bottom sheets (top corners only) |
| `pill` | 999 | Badges, tabs, plant selector, nav pill |
| `circle` | 50% | Avatars, FAB, activity icons |

### 4.3 Elevation

Soft and wide; light from directly above.

| Token | Shadow | Use |
|---|---|---|
| `0` | none | Canvas, flat surfaces |
| `1` | `0 1 3 rgba(15,23,42,0.06)` | Cards, list rows |
| `2` | `0 4 8 rgba(15,23,42,0.06)` | Dropdowns, plant selector |
| `3` | `0 8 16 rgba(15,23,42,0.08)` | FAB, bottom sheets |
| `4` | `0 16 32 rgba(15,23,42,0.10)` | Dialogs |

**Dark mode:** shadows are invisible on dark surfaces. Express elevation by surface tint instead — `1` → `#1A1D21`, `2` → `#22262B`, `3` → `#2A2E34` — plus an optional 1 dp `outline`.

### 4.4 Icons

Outline, rounded joins, ~2 dp stroke. **Material Symbols (Rounded)**, which matches the drawn style and avoids a dependency. Sizes: `sm 16` · `md 20` · `lg 24` · `xl 28`. Icon tiles are 40×40 in rows and 48×48 in KPI cards, `radius.md`, tinted container with a saturated glyph.

### 4.5 Motion

| Token | Duration | Easing | Use |
|---|---|---|---|
| `instant` | 100 ms | `EaseOut` | Press feedback |
| `fast` | 200 ms | `EaseOutCubic` | Toggles, chips, banners |
| `normal` | 250 ms | `EaseInOutCubic` | Transitions, sheets |
| `slow` | 400 ms | `EaseOutCubic` | Chart entry |
| `shimmer` | 1200 ms | `Linear` loop | Skeletons |

**Motion answers actions.** One orchestrated moment — the chart drawing in on first load — lands better than entrance animations on every card. All motion is disabled under reduced-motion; content jumps to its final state.

---

## 5. Components

### 5.1 Button

| Variant | Container | Content |
|---|---|---|
| Primary | `primary` | `onPrimary` |
| Secondary | `surface` + 1 dp `outline` | `textPrimary` |
| Tonal | `primaryContainer` | `primary` |
| Ghost | transparent | `primary` |
| Destructive | `dangerContainer` | `dangerStrong` |

Heights: `sm 36` · **`md 48` default** · `lg 56`. Radius `md`, or `pill` for the Add Item treatment. Label `labelLarge`. Icon 20, gap 8.

| State | Treatment |
|---|---|
| Pressed | `primaryPressed`, scale 0.98, 100 ms |
| Focused | 2 dp `primary` ring, 2 dp offset |
| Disabled | `#E5E7EB` container, `textDisabled`, no elevation |
| **Loading** | Spinner replaces the label; **width held at the resting width** so the layout cannot shift under a finger mid-tap; disabled; announced to TalkBack |

### 5.2 Text field

Height 48, radius `md`, padding 12/16, `bodyLarge`.

| State | Container | Border |
|---|---|---|
| Idle | `surface` | 1 dp `outline` |
| Focused | `surface` | 2 dp `primary` |
| Filled | `surface` | 1 dp `outline`, clear affordance appears |
| Error | `dangerContainer` | 2 dp `danger`, message in `bodySmall` |
| Disabled | `#F3F4F6` | 1 dp `outlineSubtle` |

The helper/error line **reserves its height always**, so focusing a field never shifts the layout below it.

### 5.3 Status badge

Pill, height 24, padding 4/10, `labelMedium`, 6 dp leading dot.

| Status | Container | Text |
|---|---|---|
| In stock | `successContainer` | `#065F46` |
| Low stock | `warningContainer` | `#92400E` |
| Out of stock | `dangerContainer` | `#991B1B` |
| Reserved | `#CFFAFE` | `#155E75` |
| In production | `#EDE9FE` | `#5B21B6` |

**The text label is mandatory.** It is the redundant channel that makes status accessible. A dot-only variant does not exist.

### 5.4 KPI card

Radius `lg`, padding 16, tinted container per category, elevation 1.

Structure: 48×48 icon tile → 12 → label (`bodyMedium`) → 4 → value (`headlineLarge`, tabular) → 8 → trend row.

**`TrendIndicator` takes an explicit `higherIsBetter` flag.** The Low Stock card shows `↑ +5` in red while three neighbours show `↑` in green. Direction cannot determine colour; the component must be told. It also renders a text qualifier — "+5 need reorder" — so the meaning survives without colour.

### 5.5 Item list row

Height ≥ 72, grows with font scale. `surface`, radius `lg`, padding 12/16, elevation 1, 8 dp between rows.

```
[ 56×56 thumb ]  Ball Bearing              [ In stock ]
                 BRG-001 · Mechanical        320 pcs
                 ⊞ Par 500 · ⌖ A1-01                    ⋮
```

Name `titleMedium` · code `codeStyle` · category `bodyMedium` `textSecondary` · meta `bodySmall` · quantity `bodyLarge` Medium, `danger` when low or out.

**Interaction fix:** the approved design places a "⋮" and a chevron close together at the right edge, both below the 48 dp minimum. **The whole row is the tap target for navigation and the chevron is removed**; the overflow keeps a full 48×48 hit area with 8 dp of separation. The row is already a navigation affordance; the chevron was decoration competing for the same space.

### 5.6 Category tab

Pill, height 40, padding 10/16, `labelLarge`, 16 dp leading icon, horizontally scrollable. Selected: `primary` on `onPrimary`. Unselected: `#EEF1F5` on `textSecondary`.

### 5.7 Settings row

Height 64, `surface`, 16 dp padding, 1 dp `outlineSubtle` between rows in a group, group radius `lg`.

40×40 tinted icon tile → 12 → title (`titleMedium`) over subtitle (`bodyMedium` `textSecondary`) → spacer → trailing value (`bodyMedium` `textSecondary`) or switch or chevron.

Switch 52×32, thumb 28. On `primary`, off `#D1D5DB`, 200 ms.

### 5.8 Bottom navigation

Height 64 + safe area, `surfaceNav`, 1 dp top `outlineSubtle`, four destinations.

Active: filled icon in `accent`, `labelMedium` in `accent`, on an `accentContainer` pill 64×32. Inactive: outline icon and label in `textSecondary`. Full-height 48 dp target per destination. Pill transition 200 ms.

### 5.9 Numeric stepper

The most-used input on Store Keeper paths.

`[ − ] [ value ] [ + ]` — buttons **56×56**, value field flexible with an 88 dp minimum, `titleLarge` tabular, centred. Long-press accelerates 1 → 10 → 100 after 1 s and 2.5 s. Direct keyboard entry on tapping the field. Bounds disable the relevant button rather than silently no-op. Haptic tick per increment, so confirmation arrives without looking.

### 5.10 Charts

**Stock composition — changed from the approved design.** The mock draws a donut of item *counts* across five exclusive statuses. Per `prd.md` §3.3 the statuses are no longer exclusive, so a count donut cannot be drawn truthfully.

Replacement: a **horizontal stacked bar of quantity**, with a legend listing each segment's label, quantity, and share. This is more truthful and more useful — a manager cares that 156 *units* are reserved, not that 156 *items* have some reservation. It also reads better at large font scales than a donut with a centred numeral, and it labels segments directly rather than relying on colour matching.

| Chart | Spec |
|---|---|
| Stacked composition | Height 32, radius `xs`, 2 dp segment gaps, direct labels, legend below |
| Area sparkline | 2.5 dp stroke `success`, gradient fill 20% → 0, terminal dot 6 dp, `bodySmall` axes, no gridlines |
| Multi-series line | 2 dp strokes, 4 dp markers with white fill, 1 dp `outlineSubtle` gridlines, legend below |

Every chart exposes a semantic summary for TalkBack and stays legible with colour removed.

### 5.11 Sync banner

Persistent inline, not a snackbar — someone who was not looking when a snackbar appeared has no way to recover the information.

| State | Container | Content |
|---|---|---|
| Offline | `warningContainer` | "Offline. Showing data from 8:45 AM." |
| Pending | `primaryContainer` | "3 changes waiting to sync" |
| Syncing | `primaryContainer` | "Syncing…" with indeterminate progress |
| Needs attention | `dangerContainer` | "1 change needs attention" → tap to resolve |
| Synced | — | Auto-dismiss after 2 s |

### 5.12 Data table

Header `surfaceAlt`, `labelMedium` `textSecondary`, 12 dp padding. Rows 48 dp, `bodyMedium`, 1 dp `outlineSubtle`. Numeric columns right-aligned, tabular. **Scrolls horizontally inside its own container**; the page body never scrolls sideways.

---

## 6. Layout

| Breakpoint | Width | Behaviour |
|---|---|---|
| Compact | < 600 dp | Single column, bottom nav, KPI row scrolls horizontally |
| Medium | 600–840 | Two-column cards, KPIs 2×2, nav stays bottom |
| Expanded | > 840 | Navigation rail, three-column dashboard, full table |

Max content width 1200 dp, centred. Vertical rhythm on 4 dp; horizontal spacing on 8 dp multiples except icon-to-label at 4.

---

## 7. Accessibility

| Requirement | Standard |
|---|---|
| Body text contrast | ≥ 4.5:1 |
| Large text, meaningful icons | ≥ 3:1 |
| Touch targets | ≥ 48 dp; 56 dp on Store Keeper paths |
| Font scaling | Functional to 200%, no clipping |
| Colour independence | Every status carries colour **and** text |
| Motion | Honours reduced-motion |
| Screen reader | Every control labelled; every chart summarised |

**Three fixes to the approved design, all implemented above:**
1. Low Stock's red `↑` against three green `↑` tiles encoded meaning in colour alone → text qualifier added (§5.4).
2. The chart legend tied dots to slices by colour only → direct segment labels (§5.10).
3. Overflow and chevron too close and too small → chevron removed, full-row tap, 48 dp overflow (§5.5).

---

## 8. Token Sketch

```kotlin
object StockProColors {
    val primary          = Color(0xFF2563EB)
    val primaryPressed   = Color(0xFF1E40AF)
    val primaryContainer = Color(0xFFEFF6FF)
    val brandMark        = Color(0xFF065F46)
    val accent           = Color(0xFF10B981)
    val accentContainer  = Color(0xFFD1FAE5)

    val success          = Color(0xFF16A34A)
    val warning          = Color(0xFFF59E0B)
    val danger           = Color(0xFFEF4444)
    val reserved         = Color(0xFF0891B2)   // was primary — see §2.5
    val production       = Color(0xFF8B5CF6)

    val canvas           = Color(0xFFF5F7FA)
    val surface          = Color(0xFFFFFFFF)
    val outline          = Color(0xFFE5E7EB)
    val textPrimary      = Color(0xFF0F172A)
    val textSecondary    = Color(0xFF6B7280)
    val textTertiary     = Color(0xFF9CA3AF)
}

object Spacing { val x1 = 4.dp; val x2 = 8.dp; val x3 = 12.dp
                 val x4 = 16.dp; val x6 = 24.dp; val x8 = 32.dp }

object Radius  { val xs = 4.dp; val sm = 8.dp; val md = 12.dp
                 val lg = 16.dp; val xl = 20.dp; val pill = 999.dp }
```

Semantic colours sit outside Material 3's `ColorScheme`, so they are exposed through one theme-aware `CompositionLocal` and consumed nowhere else. Screens call `StockProTheme.status.warning`; no screen imports a raw token. One place decides what amber means, and dark mode is handled once.

---

## 9. Design Debt Register

| # | Issue | Severity | Resolution |
|---|---|---|---|
| 1 | Brand differs: StockPro vs InventoryPro | High | Phase 0 decision |
| 2 | Nav tab 3 differs across screens | High | Phase 0 decision |
| 3 | Blue served as both action and Reserved | High | **Resolved** — Reserved → teal |
| 4 | Chart legend colour-only | High (a11y) | **Resolved** — direct labels |
| 5 | Inverted trend arrow indistinguishable without colour | High (a11y) | **Resolved** — text qualifier |
| 6 | Overflow + chevron below target size | High (a11y) | **Resolved** — chevron removed |
| 7 | Redundant Add Item button and FAB | Medium | Keep FAB, header slot → scan |
| 8 | ₹ with Western abbreviation | Medium | Locale-aware compact format |
| 9 | Unlabelled quantity on rows | Medium | Label as "Par", pending Phase 0 |
| 10 | "View All" vs "See All" | Low | "View all" |
| 11 | Two brand colours | Medium | Confirm canonical |
| 12 | No dark comps despite a shipping toggle | Medium | Phase 0 deliverable; §2.4 is derived |
| 13 | Nav bar reads faintly lavender | Low | Confirm intent vs capture artifact |
