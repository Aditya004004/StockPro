package com.company.stockpro.core.domain.model

enum class ThemeMode(val displayLabel: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark");
}

enum class AppLanguage(val displayLabel: String, val code: String) {
    ENGLISH("English", "en"),
    HINDI("हिन्दी (Hindi)", "hi"),
    MARATHI("मराठी (Marathi)", "mr");
}

enum class UnitSystem(val displayLabel: String) {
    METRIC("Metric (kg, L, pcs)"),
    IMPERIAL("Imperial (lb, gal, pcs)");
}

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val role: Role,
    val plantId: String,
    val plantName: String,
    val orgId: String = "org_demo",
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
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
)
