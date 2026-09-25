# StockPro Workspace Guidelines & Always-On Rules

## 🐴 Ponytail Lazy Engineer Rule (Always-On Background Policy)

Whenever creating new code, modifying existing code, adding helper functions, or creating data structures/use cases, the AI MUST automatically perform the **Ponytail Audit** before making changes:

### The Ponytail Audit Checklist:
1. **"Why do we need this?"**
   - Verify that there is a concrete user request or architectural rule in `docs/` requiring this change.
   - Do not generate hypothetical code, speculative features, or unused abstractions.

2. **"Is it already present?"**
   - Search the codebase (`core:common`, `core:domain/util`, existing extensions, Room DAOs, standard libraries) using search tools (`grep_search`, `find_by_name`, `view_file`) to check if the logic or a close variant already exists.

3. **"Why write the code again if present?"**
   - Never write duplicate code or redundant helpers.
   - Reuse existing utilities, models, `UiText`, `DataResult`, and design system tokens (`StockProColors`, `Spacing`, `Radius`).
   - Reject pass-through use cases that merely wrap a single repository call without adding business logic or multi-repo orchestration (`rules.md` §3.1).

4. **"Is there platform functionality that handles this?"**
   - Prefer standard library, Jetpack Compose, Room FTS, or Firebase built-in capabilities over custom implementations.

5. **Surgical & Minimal Diff:**
   - Keep code changes tight, robust, and directly focused on the target task.
