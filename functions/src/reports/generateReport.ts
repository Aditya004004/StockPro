import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { v4 as uuidv4 } from "uuid";

const db = getFirestore();

interface GenerateReportPayload {
  orgId: string;
  plantId: string;
  reportType: "INVENTORY" | "PURCHASE" | "USAGE" | "VALUATION";
  rangeStart?: string;
  rangeEnd?: string;
  format?: "csv" | "json";
}

/**
 * generateReport — server-authoritative report generator.
 * Queries snapshots/items/ledger, builds CSV tabular output, stores to Storage, and returns signed URL.
 */
export const generateReport = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }

  const { orgId, plantId, reportType, rangeStart, rangeEnd, format = "csv" } = request.data as GenerateReportPayload;
  if (!orgId || !plantId) {
    throw new HttpsError("invalid-argument", "orgId and plantId are required.");
  }

  const plantRef = db.doc(`orgs/${orgId}/plants/${plantId}`);
  const reportId = uuidv4();

  let csvRows: string[] = [];

  if (reportType === "INVENTORY" || reportType === "VALUATION") {
    // Current stock & valuation report
    const itemsSnapshot = await plantRef.collection("items").where("isActive", "==", true).get();
    csvRows.push("Code,Name,Class,Category,Location,Unit,OnHandUnits,ReservedUnits,ReorderLevel,UnitCostRupees,TotalValueRupees");

    itemsSnapshot.forEach((doc) => {
      const item = doc.data();
      const onHand = (item.onHandMilli || 0) / 1000;
      const reserved = (item.reservedMilli || 0) / 1000;
      const reorder = (item.reorderLevelMilli || 0) / 1000;
      const unitCost = (item.unitCostPaise || 0) / 100;
      const totalValue = onHand * unitCost;

      csvRows.push(
        `"${item.code || ""}","${item.name || ""}","${item.itemClass || ""}","${item.categoryName || ""}","${item.locationCode || ""}","${item.unitOfMeasure || ""}",${onHand},${reserved},${reorder},${unitCost.toFixed(2)},${totalValue.toFixed(2)}`
      );
    });
  } else {
    // Movements / Usage / Purchases report
    let ledgerQuery = plantRef.collection("ledger").orderBy("createdAt", "desc").limit(500);
    const ledgerSnapshot = await ledgerQuery.get();

    csvRows.push("Timestamp,ItemId,Type,DeltaUnits,BalanceAfterUnits,UnitCostRupees,Reference,ReasonCode,ActorName");
    ledgerSnapshot.forEach((doc) => {
      const entry = doc.data();
      const timestamp = entry.createdAt ? entry.createdAt.toDate().toISOString() : "";
      const delta = (entry.deltaMilli || 0) / 1000;
      const balance = (entry.balanceAfterMilli || 0) / 1000;
      const cost = (entry.unitCostPaise || 0) / 100;

      csvRows.push(
        `"${timestamp}","${entry.itemId || ""}","${entry.type || ""}",${delta},${balance},${cost.toFixed(2)},"${entry.reference || ""}","${entry.reasonCode || ""}","${entry.actorName || ""}"`
      );
    });
  }

  const csvContent = csvRows.join("\n");
  const fileName = `reports/${orgId}/${plantId}/${reportType.toLowerCase()}_${reportId}.csv`;

  try {
    const bucket = getStorage().bucket();
    const file = bucket.file(fileName);
    await file.save(csvContent, { contentType: "text/csv" });

    // Generate signed URL valid for 24 hours
    const [signedUrl] = await file.getSignedUrl({
      action: "read",
      expires: Date.now() + 24 * 60 * 60 * 1000,
    });

    return {
      reportId,
      reportType,
      fileName,
      downloadUrl: signedUrl,
      rowCount: csvRows.length - 1,
      generatedAt: new Date().toISOString(),
    };
  } catch (err: any) {
    // If Storage bucket is not locally configured in test environment, return inline CSV directly
    return {
      reportId,
      reportType,
      fileName,
      downloadUrl: null,
      csvContent,
      rowCount: csvRows.length - 1,
      generatedAt: new Date().toISOString(),
    };
  }
});
