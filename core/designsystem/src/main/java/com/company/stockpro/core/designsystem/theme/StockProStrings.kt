package com.company.stockpro.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.company.stockpro.core.domain.model.AppLanguage

interface StockProStringsDefinition {
    // Navigation
    val navHome: String
    val navInventory: String
    val navReports: String
    val navSettings: String

    // Common Actions & Statuses
    val close: String
    val cancel: String
    val save: String
    val confirm: String
    val delete: String
    val remove: String
    val edit: String
    val search: String
    val sync: String
    val syncing: String
    val syncNow: String
    val lastSync: String
    val justNow: String
    val pending: String
    val filter: String
    val sort: String
    val totalItems: String
    val lowStock: String
    val outOfStock: String
    val reserved: String
    val inStock: String
    val inProduction: String
    val selectPlant: String
    val addPlant: String
    val addNewPlant: String
    val plantName: String
    val plantCode: String
    val facilityLocation: String
    val removePlant: String
    val removePlantConfirm: String
    val success: String
    val error: String

    // Home Screen
    val stockOverview: String
    val totalValuation: String
    val rawMaterials: String
    val finishedGoods: String
    val quickActions: String
    val receiveStock: String
    val issueStock: String
    val auditInventory: String
    val stockLedger: String
    val recentActivity: String
    val viewAll: String
    val noRecentActivity: String
    val stockComposition: String
    val lowStockAlerts: String
    val allStockOptimal: String

    // Inventory Screen
    val searchInventoryPlaceholder: String
    val allCategories: String
    val workInProgress: String
    val packaging: String
    val maintenance: String
    val tableHeaderItem: String
    val tableHeaderSku: String
    val tableHeaderOnHand: String
    val tableHeaderLocation: String
    val tableHeaderStatus: String
    val noItemsFound: String
    val noItemsYet: String
    val addItem: String
    val scanBarcode: String
    val addNewItemTitle: String
    val createMasterDataSubtitle: String
    val itemCodeLabel: String
    val itemNameLabel: String
    val itemClassLabel: String
    val categoryLabel: String
    val uomLabel: String
    val locationBinLabel: String
    val onHandLabel: String
    val parLevelLabel: String
    val unitCostLabel: String
    val createItemButton: String
    val enterItemCodePlaceholder: String
    val enterItemNamePlaceholder: String
    val selectCategoryPlaceholder: String
    val enterLocationBinPlaceholder: String
    val lowStockTooltipDesc: String
    val parLevelTooltipDesc: String

    // Reports Screen
    val reportsTitle: String
    val monthlyTurnover: String
    val stockCompositionDist: String
    val inboundVsOutbound: String
    val inbound: String
    val outbound: String
    val valuationByCategory: String
    val topReorderPriorities: String
    val viewCatalog: String
    val noReorderPriorities: String
    val exportReportPacks: String
    val valuationLedger: String
    val purchaseHistory: String
    val usageAndIssues: String
    val reorderAudit: String
    val sharePercent: String
    val shareValue: String

    // Settings Screen
    val settingsHeader: String
    val settingsSubtitle: String
    val plantOperationsGroup: String
    val plantAndAccessControl: String
    val profileInformation: String
    val securityAndPassword: String
    val activePlantLabel: String
    val appearanceGroup: String
    val operationalPreferences: String
    val appearanceSubtitle: String
    val systemDefault: String
    val lightMode: String
    val darkMode: String
    val displayLanguage: String
    val displayLanguageSubtitle: String
    val selectLanguage: String
    val selectAppearance: String
    val measurementUnits: String
    val offlineDatabaseGroup: String
    val exportDatabase: String
    val clearCache: String
    val supportAndEnterprise: String
    val helpAndFaqs: String
    val sendFeedback: String
    val appVersion: String
    val accountActions: String
    val accountAndSecurityGroup: String
    val signOut: String
    val signOutSubtitle: String
    val deleteAccount: String
    val deleteAccountAndData: String
    val managePlants: String
    val floorAlerts: String
    val operatorProfile: String
    val editProfileTitle: String
    val editProfileSubtitle: String
    val fullNameLabel: String
    val emailAddressLabel: String
    val phoneNumberLabel: String
    val companyNameLabel: String
    val assignedPlantLabel: String
    val saveChangesButton: String

    // Movement Dialog
    val postMovementTitle: String
    val movementReceipt: String
    val movementIssue: String
    val movementTransfer: String
    val movementAdjustment: String
    val quantity: String
    val unit: String
    val sourceWarehouse: String
    val destinationWarehouse: String
    val postMovementSubmit: String
}

object EnglishStockProStrings : StockProStringsDefinition {
    override val navHome = "Home"
    override val navInventory = "Inventory"
    override val navReports = "Reports"
    override val navSettings = "Settings"

    override val close = "Close"
    override val cancel = "Cancel"
    override val save = "Save"
    override val confirm = "Confirm"
    override val delete = "Delete"
    override val remove = "Remove"
    override val edit = "Edit"
    override val search = "Search"
    override val sync = "Sync"
    override val syncing = "Syncing"
    override val syncNow = "Sync Now"
    override val lastSync = "LAST SYNC"
    override val justNow = "Just now"
    override val pending = "Pending"
    override val filter = "Filter"
    override val sort = "Sort"
    override val totalItems = "Total Items"
    override val lowStock = "Low Stock"
    override val outOfStock = "Out of Stock"
    override val reserved = "Reserved"
    override val inStock = "In Stock"
    override val inProduction = "In Production"
    override val selectPlant = "Select Plant"
    override val addPlant = "Add Plant"
    override val addNewPlant = "Add New Plant"
    override val plantName = "Plant Name"
    override val plantCode = "Plant Code"
    override val facilityLocation = "Facility Location"
    override val removePlant = "Remove Plant"
    override val removePlantConfirm = "Are you sure you want to remove this plant?"
    override val success = "Success"
    override val error = "Error"

    override val stockOverview = "STOCK OVERVIEW"
    override val totalValuation = "Total Valuation"
    override val rawMaterials = "Raw Materials"
    override val finishedGoods = "Finished Goods"
    override val quickActions = "QUICK ACTIONS"
    override val receiveStock = "Receive Stock"
    override val issueStock = "Issue Stock"
    override val auditInventory = "Audit Inventory"
    override val stockLedger = "Stock Ledger"
    override val recentActivity = "RECENT ACTIVITY"
    override val viewAll = "View All"
    override val noRecentActivity = "No recent activity"
    override val stockComposition = "STOCK COMPOSITION"
    override val lowStockAlerts = "LOW STOCK ALERTS"
    override val allStockOptimal = "All stock optimal"

    override val searchInventoryPlaceholder = "Search Item Name, SKU, Category, Warehouse..."
    override val allCategories = "All"
    override val workInProgress = "Work in Progress"
    override val packaging = "Packaging"
    override val maintenance = "Maintenance"
    override val tableHeaderItem = "ITEM"
    override val tableHeaderSku = "SKU"
    override val tableHeaderOnHand = "ON HAND"
    override val tableHeaderLocation = "LOCATION"
    override val tableHeaderStatus = "STATUS"
    override val noItemsFound = "No items found"
    override val noItemsYet = "No items yet"
    override val addItem = "Add Item"
    override val scanBarcode = "Scan Barcode"
    override val addNewItemTitle = "Add New Item"
    override val createMasterDataSubtitle = "Create master data entry for inventory tracking"
    override val itemCodeLabel = "Item Code"
    override val itemNameLabel = "Item Name"
    override val itemClassLabel = "Item Class"
    override val categoryLabel = "Category"
    override val uomLabel = "UOM"
    override val locationBinLabel = "Location / Bin"
    override val onHandLabel = "On Hand"
    override val parLevelLabel = "Par Level"
    override val unitCostLabel = "Unit Cost"
    override val createItemButton = "Create Item"
    override val enterItemCodePlaceholder = "Enter item code"
    override val enterItemNamePlaceholder = "Enter item name"
    override val selectCategoryPlaceholder = "Select category"
    override val enterLocationBinPlaceholder = "Enter location or bin"
    override val lowStockTooltipDesc = "Low Stock: Alert trigger threshold (min stock level)"
    override val parLevelTooltipDesc = "Par Level: Target inventory level to restore on replenishment"

    override val reportsTitle = "REPORTS & ANALYTICS"
    override val monthlyTurnover = "Monthly material turnover velocity"
    override val stockCompositionDist = "STOCK COMPOSITION DISTRIBUTION"
    override val inboundVsOutbound = "INBOUND VS OUTBOUND FLOW"
    override val inbound = "Inbound"
    override val outbound = "Outbound"
    override val valuationByCategory = "VALUATION BY CATEGORY"
    override val topReorderPriorities = "TOP REORDER PRIORITIES"
    override val viewCatalog = "View Catalog >"
    override val noReorderPriorities = "No reorder priorities"
    override val exportReportPacks = "EXPORT REPORT PACKS"
    override val valuationLedger = "Valuation\nLedger"
    override val purchaseHistory = "Purchase\nHistory"
    override val usageAndIssues = "Usage &\nIssues"
    override val reorderAudit = "Reorder\nAudit"
    override val sharePercent = "% Share"
    override val shareValue = "Value"

    override val settingsHeader = "SETTINGS"
    override val settingsSubtitle = "Plant configuration & preferences"
    override val plantOperationsGroup = "PLANT OPERATIONS"
    override val plantAndAccessControl = "PLANT & ACCESS CONTROL"
    override val profileInformation = "Profile Information"
    override val securityAndPassword = "Security & Password"
    override val activePlantLabel = "Active Facility / Plant"
    override val appearanceGroup = "APPEARANCE"
    override val operationalPreferences = "OPERATIONAL PREFERENCES"
    override val appearanceSubtitle = "Customize system behavior"
    override val systemDefault = "System Default"
    override val lightMode = "Light"
    override val darkMode = "Dark"
    override val displayLanguage = "Display Language"
    override val displayLanguageSubtitle = "Interface localization (English, Hindi)"
    override val selectLanguage = "Select Language"
    override val selectAppearance = "Select Appearance"
    override val measurementUnits = "Measurement Units"
    override val offlineDatabaseGroup = "OFFLINE DATABASE & CACHE"
    override val exportDatabase = "Export Database"
    override val clearCache = "Clear Application Cache"
    override val supportAndEnterprise = "SUPPORT & ENTERPRISE POLICIES"
    override val helpAndFaqs = "Shop Floor Help & FAQs"
    override val sendFeedback = "Send Operator Feedback"
    override val appVersion = "StockPro Version"
    override val accountActions = "ACCOUNT ACTIONS"
    override val accountAndSecurityGroup = "ACCOUNT & SECURITY"
    override val signOut = "Sign Out"
    override val signOutSubtitle = "Sign out of your active session on this device"
    override val deleteAccount = "Delete Account"
    override val deleteAccountAndData = "Delete Account & Purge Local Data"
    override val managePlants = "Manage Plants"
    override val floorAlerts = "Plant Floor Alerts"
    override val operatorProfile = "Operator Profile"
    override val editProfileTitle = "Edit Operator Profile"
    override val editProfileSubtitle = "Update your personal and plant details"
    override val fullNameLabel = "Full Name"
    override val emailAddressLabel = "Email Address"
    override val phoneNumberLabel = "Phone Number"
    override val companyNameLabel = "Company Name"
    override val assignedPlantLabel = "Assigned Plant"
    override val saveChangesButton = "Save Changes"

    override val postMovementTitle = "Post Stock Movement"
    override val movementReceipt = "Stock In (Receipt)"
    override val movementIssue = "Stock Out (Issue)"
    override val movementTransfer = "Transfer"
    override val movementAdjustment = "Adjustment"
    override val quantity = "Quantity"
    override val unit = "Unit"
    override val sourceWarehouse = "Source Warehouse"
    override val destinationWarehouse = "Destination Warehouse"
    override val postMovementSubmit = "Post Movement"
}

object HindiStockProStrings : StockProStringsDefinition {
    override val navHome = "होम"
    override val navInventory = "इन्वेंटरी"
    override val navReports = "रिपोर्ट्स"
    override val navSettings = "सेटिंग्स"

    override val close = "बंद करें"
    override val cancel = "रद्द करें"
    override val save = "सहेजें"
    override val confirm = "पुष्टि करें"
    override val delete = "हटाएं"
    override val remove = "हटाएं"
    override val edit = "संपादित करें"
    override val search = "खोजें"
    override val sync = "सिंक"
    override val syncing = "सिंक हो रहा है"
    override val syncNow = "अभी सिंक करें"
    override val lastSync = "अंतिम सिंक"
    override val justNow = "अभी-अभी"
    override val pending = "लंबित"
    override val filter = "फ़िल्टर"
    override val sort = "क्रमबद्ध करें"
    override val totalItems = "कुल वस्तुएं"
    override val lowStock = "कम स्टॉक"
    override val outOfStock = "स्टॉक समाप्त"
    override val reserved = "आरक्षित"
    override val inStock = "स्टॉक में"
    override val inProduction = "उत्पादन में"
    override val selectPlant = "प्लांट चुनें"
    override val addPlant = "प्लांट जोड़ें"
    override val addNewPlant = "नया प्लांट जोड़ें"
    override val plantName = "प्लांट का नाम"
    override val plantCode = "प्लांट कोड"
    override val facilityLocation = "सुविधा स्थान"
    override val removePlant = "प्लांट हटाएं"
    override val removePlantConfirm = "क्या आप वाकई इस प्लांट को हटाना चाहते हैं?"
    override val success = "सफल"
    override val error = "त्रुटि"

    override val stockOverview = "स्टॉक अवलोकन"
    override val totalValuation = "कुल मूल्यांकन"
    override val rawMaterials = "कच्चा माल"
    override val finishedGoods = "तैयार माल"
    override val quickActions = "त्वरित कार्य"
    override val receiveStock = "स्टॉक प्राप्त करें"
    override val issueStock = "स्टॉक जारी करें"
    override val auditInventory = "इन्वेंटरी ऑडिट"
    override val stockLedger = "स्टॉक बहीखाता"
    override val recentActivity = "हाल की गतिविधि"
    override val viewAll = "सभी देखें"
    override val noRecentActivity = "कोई हाल की गतिविधि नहीं"
    override val stockComposition = "स्टॉक संरचना"
    override val lowStockAlerts = "कम स्टॉक चेतावनी"
    override val allStockOptimal = "सभी स्टॉक इष्टतम है"

    override val searchInventoryPlaceholder = "आइटम का नाम, SKU, श्रेणी, वेयरहाउस खोजें..."
    override val allCategories = "सभी"
    override val workInProgress = "प्रगति पर कार्य"
    override val packaging = "पैकेजिंग"
    override val maintenance = "रखरखाव"
    override val tableHeaderItem = "वस्तु"
    override val tableHeaderSku = "SKU"
    override val tableHeaderOnHand = "उपलब्ध"
    override val tableHeaderLocation = "स्थान"
    override val tableHeaderStatus = "स्थिति"
    override val noItemsFound = "कोई वस्तु नहीं मिली"
    override val noItemsYet = "अभी कोई वस्तु नहीं है"
    override val addItem = "वस्तु जोड़ें"
    override val scanBarcode = "बारकोड स्कैन करें"
    override val addNewItemTitle = "नया आइटम जोड़ें"
    override val createMasterDataSubtitle = "इन्वेंटरी ट्रैकिंग के लिए मास्टर डेटा एंट्री बनाएं"
    override val itemCodeLabel = "आइटम कोड"
    override val itemNameLabel = "आइटम का नाम"
    override val itemClassLabel = "आइटम वर्ग"
    override val categoryLabel = "श्रेणी"
    override val uomLabel = "इकाई (UOM)"
    override val locationBinLabel = "स्थान / बिन"
    override val onHandLabel = "हाथ में स्टॉक"
    override val parLevelLabel = "पार स्तर"
    override val unitCostLabel = "इकाई लागत"
    override val createItemButton = "आइटम बनाएं"
    override val enterItemCodePlaceholder = "आइटम कोड दर्ज करें"
    override val enterItemNamePlaceholder = "आइटम का नाम दर्ज करें"
    override val selectCategoryPlaceholder = "श्रेणी चुनें"
    override val enterLocationBinPlaceholder = "स्थान या बिन दर्ज करें"
    override val lowStockTooltipDesc = "कम स्टॉक: अलर्ट ट्रिगर सीमा (न्यूनतम स्टॉक स्तर)"
    override val parLevelTooltipDesc = "पार स्तर: पुनःपूर्ति पर पुनर्स्थापित करने के लिए लक्षित स्तर"

    override val reportsTitle = "रिपोर्ट्स और विश्लेषण"
    override val monthlyTurnover = "मासिक सामग्री टर्नओवर गति"
    override val stockCompositionDist = "स्टॉक संरचना वितरण"
    override val inboundVsOutbound = "आवक बनाम जावक प्रवाह"
    override val inbound = "आवक"
    override val outbound = "जावक"
    override val valuationByCategory = "श्रेणी अनुसार मूल्यांकन"
    override val topReorderPriorities = "शीर्ष पुनः ऑर्डर प्राथमिकताएं"
    override val viewCatalog = "कैटलॉग देखें >"
    override val noReorderPriorities = "कोई पुनः ऑर्डर प्राथमिकता नहीं"
    override val exportReportPacks = "रिपोर्ट पैक निर्यात करें"
    override val valuationLedger = "मूल्यांकन\nबहीखाता"
    override val purchaseHistory = "खरीद\nइतिहास"
    override val usageAndIssues = "उपयोग और\nजारी"
    override val reorderAudit = "पुनः ऑर्डर\nऑडिट"
    override val sharePercent = "% हिस्सा"
    override val shareValue = "मूल्य"

    override val settingsHeader = "सेटिंग्स"
    override val settingsSubtitle = "प्लांट कॉन्फ़िगरेशन और प्राथमिकताएं"
    override val plantOperationsGroup = "प्लांट संचालन"
    override val plantAndAccessControl = "प्लांट और एक्सेस नियंत्रण"
    override val profileInformation = "प्रोफ़ाइल जानकारी"
    override val securityAndPassword = "सुरक्षा और पासवर्ड"
    override val activePlantLabel = "सक्रिय सुविधा / प्लांट"
    override val appearanceGroup = "दिखावट"
    override val operationalPreferences = "परिचालन प्राथमिकताएं"
    override val appearanceSubtitle = "सिस्टम व्यवहार कस्टमाइज़ करें"
    override val systemDefault = "सिस्टम डिफ़ॉल्ट"
    override val lightMode = "लाइट"
    override val darkMode = "डार्क"
    override val displayLanguage = "भाषा"
    override val displayLanguageSubtitle = "इंटरफ़ेस भाषा (अंग्रेज़ी, हिन्दी)"
    override val selectLanguage = "भाषा चुनें"
    override val selectAppearance = "दिखावट चुनें"
    override val measurementUnits = "माप इकाइयां"
    override val offlineDatabaseGroup = "ऑफ़लाइन डेटाबेस और कैश"
    override val exportDatabase = "डेटाबेस निर्यात करें"
    override val clearCache = "एप्लिकेशन कैश साफ़ करें"
    override val supportAndEnterprise = "सहायता और नीतियां"
    override val helpAndFaqs = "शॉप फ़्लोर सहायता और अक्सर पूछे जाने वाले प्रश्न"
    override val sendFeedback = "ऑपरेटर फ़ीडबैक भेजें"
    override val appVersion = "StockPro संस्करण"
    override val accountActions = "खाता क्रियाएं"
    override val accountAndSecurityGroup = "खाता और सुरक्षा"
    override val signOut = "साइन आउट"
    override val signOutSubtitle = "इस डिवाइस पर सक्रिय सत्र से साइन आउट करें"
    override val deleteAccount = "खाता हटाएं"
    override val deleteAccountAndData = "खाता हटाएं और स्थानीय डेटा साफ़ करें"
    override val managePlants = "प्लांट प्रबंधित करें"
    override val floorAlerts = "प्लांट फ़्लोर अलर्ट"
    override val operatorProfile = "ऑपरेटर प्रोफ़ाइल"
    override val editProfileTitle = "ऑपरेटर प्रोफ़ाइल संपादित करें"
    override val editProfileSubtitle = "अपने व्यक्तिगत और प्लांट विवरण अपडेट करें"
    override val fullNameLabel = "पूरा नाम"
    override val emailAddressLabel = "ईमेल पता"
    override val phoneNumberLabel = "फ़ोन नंबर"
    override val companyNameLabel = "कंपनी का नाम"
    override val assignedPlantLabel = "निर्दिष्ट प्लांट"
    override val saveChangesButton = "परिवर्तन सहेजें"

    override val postMovementTitle = "स्टॉक गतिविधि दर्ज करें"
    override val movementReceipt = "स्टॉक आवक (प्राप्ति)"
    override val movementIssue = "स्टॉक जावक (जारी)"
    override val movementTransfer = "स्थानांतरण"
    override val movementAdjustment = "समायोजन"
    override val quantity = "मात्रा"
    override val unit = "इकाई"
    override val sourceWarehouse = "स्रोत वेयरहाउस"
    override val destinationWarehouse = "गंतव्य वेयरहाउस"
    override val postMovementSubmit = "गतिविधि दर्ज करें"
}

val LocalStockProStrings = staticCompositionLocalOf<StockProStringsDefinition> { EnglishStockProStrings }

object StockProStrings {
    val current: StockProStringsDefinition
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProStrings.current

    fun from(language: AppLanguage): StockProStringsDefinition = when (language) {
        AppLanguage.HINDI -> HindiStockProStrings
        AppLanguage.ENGLISH -> EnglishStockProStrings
    }
}
