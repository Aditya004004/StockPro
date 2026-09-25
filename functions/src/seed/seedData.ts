import { getFirestore, FieldValue } from "firebase-admin/firestore";

/**
 * Seed data fixture for development and emulator environments (phases.md § Phase 2).
 * Seeds 1 organization, 2 plants, categories, sample items across all classes, and ledger entries.
 */
export async function seedDatabase(db: ReturnType<typeof getFirestore>) {
  const orgId = "org_demo";
  const orgRef = db.collection("organizations").doc(orgId);

  await orgRef.set({
    name: "Apex Precision Manufacturing Ltd.",
    plan: "ENTERPRISE",
    createdAt: FieldValue.serverTimestamp(),
    settings: {
      currency: "INR",
      timezone: "Asia/Kolkata",
    },
  });

  const plants = [
    { id: "plant_01", code: "PUN-01", name: "Pune Fabrication Plant", timezone: "Asia/Kolkata", currency: "INR" },
    { id: "plant_02", code: "CHN-02", name: "Chennai Moulding Plant", timezone: "Asia/Kolkata", currency: "INR" },
  ];

  for (const plant of plants) {
    const plantRef = orgRef.collection("plants").doc(plant.id);
    await plantRef.set({
      ...plant,
      createdAt: FieldValue.serverTimestamp(),
    });

    // Locations
    const locations = [
      { id: "loc_a1", code: "A1-01", zone: "Zone A", rack: "1", bin: "01" },
      { id: "loc_b2", code: "B2-04", zone: "Zone B", rack: "2", bin: "04" },
      { id: "loc_t1", code: "TOOL-R1", zone: "Toolroom", rack: "T1", bin: "01" },
    ];
    for (const loc of locations) {
      await plantRef.collection("locations").doc(loc.id).set(loc);
    }

    // Categories
    const categories = [
      { id: "cat_comp", name: "Components", itemClass: "COMPONENT" },
      { id: "cat_raw", name: "Raw Material", itemClass: "RAW_MATERIAL" },
      { id: "cat_fg", name: "Finished Goods", itemClass: "FINISHED_GOOD" },
      { id: "cat_tools", name: "Tooling", itemClass: "TOOL" },
    ];
    for (const cat of categories) {
      await plantRef.collection("categories").doc(cat.id).set(cat);
    }

    // Items
    const sampleItems = [
      {
        id: `${plant.id}_item_01`,
        code: "BRG-001",
        codeLower: "brg-001",
        name: "Deep Groove Ball Bearing 6205",
        nameLower: "deep groove ball bearing 6205",
        itemClass: "COMPONENT",
        categoryId: "cat_comp",
        categoryName: "Components",
        unitOfMeasure: "PCS",
        onHandMilli: 320_000, // 320 pcs
        reservedMilli: 20_000,
        inProductionMilli: 0,
        reorderLevelMilli: 50_000, // 50 pcs
        parLevelMilli: 500_000,
        unitCostPaise: 45_000, // ₹450.00
        locationId: "loc_a1",
        locationCode: "A1-01",
        isActive: true,
      },
      {
        id: `${plant.id}_item_02`,
        code: "STL-202",
        codeLower: "stl-202",
        name: "EN8 Carbon Steel Round Bar 25mm",
        nameLower: "en8 carbon steel round bar 25mm",
        itemClass: "RAW_MATERIAL",
        categoryId: "cat_raw",
        categoryName: "Raw Material",
        unitOfMeasure: "KG",
        onHandMilli: 1_250_000, // 1,250 kg
        reservedMilli: 100_000,
        inProductionMilli: 0,
        reorderLevelMilli: 300_000,
        parLevelMilli: 2_000_000,
        unitCostPaise: 7_800, // ₹78.00 / kg
        locationId: "loc_b2",
        locationCode: "B2-04",
        isActive: true,
      },
      {
        id: `${plant.id}_item_03`,
        code: "MOLD-4C",
        codeLower: "mold-4c",
        name: "Cap Enclosure 4-Cavity Injection Mould",
        nameLower: "cap enclosure 4-cavity injection mould",
        itemClass: "TOOL",
        categoryId: "cat_tools",
        categoryName: "Tooling",
        unitOfMeasure: "PCS",
        onHandMilli: 2_000, // 2 units
        reservedMilli: 0,
        inProductionMilli: 0,
        reorderLevelMilli: 1_000,
        parLevelMilli: 3_000,
        unitCostPaise: 350_000_00, // ₹3,50,000.00
        locationId: "loc_t1",
        locationCode: "TOOL-R1",
        isActive: true,
      },
    ];

    for (const item of sampleItems) {
      const itemRef = plantRef.collection("items").doc(item.id);
      await itemRef.set({
        ...item,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
        lastMovementAt: FieldValue.serverTimestamp(),
      });

      // Initial receipt ledger entry
      await plantRef.collection("ledger").doc(`seed_ledger_${item.id}`).set({
        itemId: item.id,
        itemCode: item.code,
        itemName: item.name,
        type: "RECEIPT",
        deltaMilli: item.onHandMilli,
        balanceAfterMilli: item.onHandMilli,
        unitCostPaise: item.unitCostPaise,
        reference: "PO-INIT-001",
        reasonCode: null,
        note: "Initial plant stock count",
        actorUid: "system_seed",
        actorName: "System Seed",
        idempotencyKey: `seed_key_${item.id}`,
        createdAt: FieldValue.serverTimestamp(),
      });
    }
  }
}
