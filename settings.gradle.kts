pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StockPro"
include(":app")

// Core modules
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:designsystem")
include(":core:ui")

// Feature modules
include(":feature:auth")
include(":feature:home")
include(":feature:inventory")
include(":feature:movements")
include(":feature:reports")
include(":feature:settings")
