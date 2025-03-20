// Apply the IA Module SDK plugin for building Ignition modules
// and the Eclipse plugin for better IDE support
plugins {
    // The main plugin that provides tasks for building and deploying Ignition modules
	id("io.ia.sdk.modl") version("0.3.0")
    // Added for better IDE support with Eclipse & VS Code
    id("eclipse")
}

// Configure settings that apply to all projects in the build
allprojects {
    // Set the version for all projects. Used in artifact naming and module version
    version = "0.0.1-SNAPSHOT"
    // Apply the eclipse plugin to all projects for consistent IDE support
    apply(plugin = "eclipse")
}

ignitionModule {
    name.set("Tag CICD")
    fileName.set("Tag-CICD.modl")
    id.set("tag-cicd")
    moduleVersion.set("${project.version}")
    license.set("LICENSE.txt")
    moduleDescription.set("A module to provide Tag related CICD Endpoints")
    requiredIgnitionVersion.set("8.1.24")

    projectScopes.set(mapOf(
        ":common" to "GC",
        ":gateway" to "G"
    ))

    moduleDependencies.set(mapOf())

    hooks.set(mapOf(
        "com.bwdesigngroup.ignition.tag_cicd.gateway.TagCICDGatewayHook" to "G"
    ))

    applyInductiveArtifactRepo.set(true)
    skipModlSigning.set(!findProperty("signModule").toString().toBoolean())

}

// Configure the Deploy task provided by the IA plugin
tasks.withType<io.ia.sdk.gradle.modl.task.Deploy>().configureEach {
    // Set the target gateway URL from gradle.properties or empty if not defined
    hostGateway.set(project.findProperty("hostGateway")?.toString() ?: "")
}

// Custom task for deep cleaning the project
val deepClean by tasks.registering {
    // Make this task depend on the clean task of all subprojects
    dependsOn(allprojects.map { "${it.path}:clean" })
    description = "Executes clean tasks and remove node plugin caches."
    // Additionally remove the Gradle cache directory
    doLast {
        delete(file(".gradle"))
    }
}