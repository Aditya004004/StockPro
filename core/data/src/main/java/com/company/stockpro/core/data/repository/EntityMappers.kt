package com.company.stockpro.core.data.repository

import com.company.stockpro.core.database.entity.ItemEntity
import com.company.stockpro.core.database.entity.StockLedgerEntity
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.StockMovement

internal fun ItemEntity.toDomain(): Item = Item(
    id = id,
    code = code,
    name = name,
    itemClass = ItemClass.fromString(itemClass),
    categoryName = categoryName,
    unitOfMeasure = unitOfMeasure,
    onHand = Quantity(onHandMilli),
    reserved = Quantity(reservedMilli),
    inProduction = Quantity(inProductionMilli),
    reorderLevel = Quantity(reorderLevelMilli),
    parLevel = Quantity(parLevelMilli),
    unitCost = Money(unitCostPaise),
    locationCode = locationCode,
    barcode = barcode,
    imagePath = imagePath,
    isActive = isActive,
    lastMovementAt = lastMovementAt,
)

internal fun StockLedgerEntity.toDomain(): StockMovement = StockMovement(
    id = id,
    itemId = itemId,
    itemCode = itemCode,
    itemName = itemName,
    type = MovementType.fromString(type),
    delta = Quantity(deltaMilli),
    balanceAfter = Quantity(balanceAfterMilli),
    unitCost = Money(unitCostPaise),
    reference = reference,
    reasonCode = reasonCode,
    note = note,
    fromLocationId = fromLocationId,
    toLocationId = toLocationId,
    actorUid = actorUid,
    actorName = actorName,
    idempotencyKey = idempotencyKey,
    clientCreatedAt = clientCreatedAt,
    createdAt = createdAt,
    isPending = isPending,
)
