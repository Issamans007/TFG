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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TFG"
include(":app")
include(":core")
include(":domain")
include(":data-local")
include(":data-remote")
include(":security")
include(":trading-engine")
include(":feature-auth")
include(":feature-dashboard")
include(":feature-markets")
include(":feature-trade")
include(":feature-chart")
include(":feature-script")
include(":feature-portfolio")
include(":feature-settings")
include(":feature-risk")
include(":feature-notifications")
include(":feature-alerts")
 