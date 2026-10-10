package com.company.stockpro.core.domain.model

enum class ThemeMode(val displayLabel: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark");
}

enum class AppLanguage(val displayLabel: String, val code: String) {
    ENGLISH("English", "en"),
    HINDI("हिन्दी (Hindi)", "hi");
}

enum class UnitSystem(
    val displayLabel: String,
    val weightUnit: String = "kg",
    val volumeUnit: String = "L",
) {
    METRIC("Metric (kg, L, pcs)", "kg", "L"),
    IMPERIAL("Imperial (lb, gal, pcs)", "lb", "gal");

    fun convertWeightKg(kg: Double): Double = when (this) {
        METRIC -> kg
        IMPERIAL -> kg * 2.20462262
    }

    fun convertVolumeLiters(liters: Double): Double = when (this) {
        METRIC -> liters
        IMPERIAL -> liters * 0.26417205
    }
}

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val role: Role,
    val plantId: String,
    val plantName: String,
    val orgId: String = "org_demo",
    val warehouseName: String = "Main Warehouse",
    val phone: String = "+91 98765 43210",
    val company: String = "StockPro Manufacturing Ltd",
    val lastLoginMillis: Long = 1711958400000L,
) {
    val initials: String
        get() {
            val parts = displayName.trim().split("\\s+".toRegex())
            return when {
                parts.isEmpty() || parts[0].isEmpty() -> "U"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts[0].first()}${parts[1].first()}".uppercase()
            }
        }
}

data class UserSettings(
    val profile: UserProfile,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val currency: Currency = Currency.INR,
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val availablePlants: List<String> = listOf("Pune Plant 1", "Plant 01 Production", "Chakan Assembly"),
)
