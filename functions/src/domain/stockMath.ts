/**
 * StockPro shared domain math & permission evaluation (mirrored from Kotlin domain layer).
 * architecture.md §5.3, §4.2
 */

export function deriveStockLevel(balanceMilli: number, reorderLevelMilli: number): string {
  if (balanceMilli <= 0) return "OUT_OF_STOCK";
  if (balanceMilli <= reorderLevelMilli) return "LOW_STOCK";
  return "IN_STOCK";
}

/**
 * Permission matrix check for stock movements (architecture.md §5.3).
 */
export function isMovementAllowed(role: string, type: string): boolean {
  const permissions: Record<string, string[]> = {
    STORE_KEEPER: ["RECEIPT", "ISSUE", "TRANSFER_IN", "TRANSFER_OUT"],
    PRODUCTION_SUPERVISOR: ["ISSUE", "PRODUCTION", "SCRAP"],
    QA: ["SCRAP"],
    INVENTORY_MANAGER: ["RECEIPT", "ISSUE", "PRODUCTION", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT", "SCRAP"],
    PLANT_MANAGER: ["RECEIPT", "ISSUE", "PRODUCTION", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT", "SCRAP"],
    ORG_ADMIN: ["RECEIPT", "ISSUE", "PRODUCTION", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT", "SCRAP"],
  };

  const allowed = permissions[role];
  return allowed ? allowed.includes(type) : false;
}

/**
 * Permission check for master data updates: create and update item (architecture.md §5.3).
 */
export function isMasterDataAllowed(role: string): boolean {
  return ["INVENTORY_MANAGER", "PLANT_MANAGER", "ORG_ADMIN"].includes(role);
}

/**
 * Permission check for deactivating items (architecture.md §5.3).
 */
export function isDeactivationAllowed(role: string): boolean {
  return ["PLANT_MANAGER", "ORG_ADMIN"].includes(role);
}

/**
 * Permission check for team management: invite, update role, deactivate (architecture.md §5.3).
 */
export function isTeamManagementAllowed(role: string): boolean {
  return ["PLANT_MANAGER", "ORG_ADMIN"].includes(role);
}
