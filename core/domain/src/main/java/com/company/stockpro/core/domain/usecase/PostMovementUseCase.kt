package com.company.stockpro.core.domain.usecase

import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.core.domain.util.UiText
import java.util.UUID

/**
 * Validates a stock movement, generates an idempotency key, and coordinates
 * the optimistic Room apply + outbox enqueue.
 *
 * This use case earns its existence (rules.md §3.1) because it:
 * 1. Enforces business rules (quantity > 0, available stock check for ISSUE,
 *    mandatory reason code for ADJUSTMENT/SCRAP).
 * 2. Generates the idempotency UUID.
 * 3. Orchestrates the repository write.
 */
class PostMovementUseCase(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(movement: StockMovement): DataResult<StockMovement> {
        // ── 1. Validate delta > 0 (Adjustments use signed deltas, so skip for those) ──
        if (movement.type != MovementType.ADJUSTMENT && movement.delta.milli <= 0L) {
            return DataResult.Error(
                message = UiText.Dynamic("Quantity must be greater than zero."),
            )
        }

        // ── 2. ISSUE / TRANSFER_OUT: check available stock ──
        if (movement.type == MovementType.ISSUE || movement.type == MovementType.TRANSFER_OUT) {
            when (val itemResult = repository.getItem(movement.itemId)) {
                is DataResult.Success -> {
                    val available = itemResult.data.available
                    if (movement.delta.milli > available.milli) {
                        return DataResult.Error(
                            message = UiText.Dynamic(
                                "Insufficient stock. Only ${available.toUnitsDouble} available."
                            ),
                        )
                    }
                }
                is DataResult.Error -> return itemResult
            }
        }

        // ── 3. ADJUSTMENT / SCRAP: require reason code ──
        if (movement.type == MovementType.ADJUSTMENT || movement.type == MovementType.SCRAP) {
            if (movement.reasonCode.isNullOrBlank()) {
                return DataResult.Error(
                    message = UiText.Dynamic("A reason code is required for adjustments and scrap."),
                )
            }
        }

        // ── 4. Generate idempotency key ──
        val withKey = movement.copy(
            idempotencyKey = UUID.randomUUID().toString(),
            isPending = true,
        )

        // ── 5. Delegate to repository: optimistic Room apply + outbox enqueue ──
        return repository.postMovement(withKey)
    }
}
