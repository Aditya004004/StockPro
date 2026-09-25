# Role-Specific Shop-Floor Training Guide

**Audience:** Manufacturing Plant Operations Staff  
**Standard:** `docs/phases.md` § Phase 7 (Training material for each role)

---

## 1. Store Keeper Cheat Sheet

**Key Responsibilities:** Receiving supplier goods, issuing items to assembly lines, bin transfer.

### Workflow A: Receiving Inbound Stock
1. Tap **Receive Stock** (+ button on Home or FAB on Inventory).
2. Point camera at PO / Supplier Box barcode (or type Code).
3. Confirm item name, verify quantity with physical package count.
4. Input Inbound Quantity and optional PO Reference / Invoice #.
5. Tap **Confirm Receipt**.
   - *Confirmation sound plays; local balance updates immediately even if offline.*

### Workflow B: Issuing Stock to Production
1. Tap **Issue Stock**.
2. Scan item barcode or search item name.
3. Review **Available Stock** (app prevents issuing more than on-hand).
4. Enter issue quantity and line/operator destination.
5. Tap **Post Issue**.

---

## 2. Production Supervisor Cheat Sheet

**Key Responsibilities:** Requesting component replenishment, recording WIP / Finished Goods production.

1. Navigate to **Inventory → Finished Goods**.
2. Tap **Post Movement → Production**.
3. Enter completed assembly count and work order number.
4. Tap **Confirm Production**.

---

## 3. QA Inspector Cheat Sheet

**Key Responsibilities:** Tool calibration validation, quarantining defective stock, recording Scrap.

### Workflow A: Logging Tool Calibration
1. Navigate to **Inventory → Tools**.
2. Tap tool row → scroll to **Tool Custody & Calibration**.
3. Verify status badge (`VALID`, `DUE SOON`, `OVERDUE`).
4. Tap **Log Calibration** → Enter Certificate Number, Calibrated By, and New Expiry Date.
5. Tap **Record Calibration**.

### Workflow B: Scrap Movement
1. Locate rejected batch item.
2. Tap **Post Movement → Scrap**.
3. Enter scrap count and mandatory **Reason Code** (e.g. `DIMENSIONAL_DEFECT`, `HEAT_DAMAGE`).

---

## 4. Inventory Manager & Plant Manager Cheat Sheet

**Key Responsibilities:** Cycle counts, adjusting discrepancies, reviewing valuation, managing team members.

### Workflow A: Cycle Count Stock Adjustment
1. Select item → tap **Post Movement → Adjust**.
2. Enter physical count delta (positive or negative).
3. Select standard adjustment reason (`CYCLE_COUNT_DISCREPANCY`, `DAMAGE`, `RETURN`).
4. *Note: Adjustments > ₹10,000 variance automatically generate high-priority management audit notifications.*

### Workflow B: Team Member Invitation & Role Changes
1. Go to **Settings → Team Management**.
2. Tap **Invite Member (+)** → Enter corporate email and select role.
3. To update role: Tap **⋮ → Change Role** → Select new role. Token is immediately revoked and renewed with new permissions.
