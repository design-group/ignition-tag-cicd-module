// Configure how Gradle finds and uses plugins
pluginManagement {
    repositories {
        gradlePluginPortal()  // Standard Gradle plugin repository
        mavenCentral()        // Central Maven repository
        mavenLocal()          // Local Maven repository
        // add the IA repo to pull in the module-signer artifact.  Can be removed if the module-signer is maven
        // published locally from its source-code and loaded via mavenLocal.
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public/")
        }
    }
}

// Configure how Gradle resolves dependencies
dependencyResolutionManagement {
    // Prefer repositories defined in settings over project-specific ones
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenLocal()  // Check local Maven repository first
        // IA's repository for SDK dependencies
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public/")
        }
    }
}

// Enable type-safe project accessors for cleaner build scripts
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "tag-cicd"

include(":common", ":gateway", ":designer")