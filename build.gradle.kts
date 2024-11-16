plugins {
    id("io.ia.sdk.modl") version "0.1.1"
}

val sdk_version by extra("8.1.24")

allprojects {
    // cascade version, which will be set at command line in CI, down to subprojects
    version = rootProject.version
}

ignitionModule {
    name.set("Tag CICD")
    fileName.set("Tag-CICD.modl")
    id.set("tag-cicd")
    moduleVersion.set(version.toString())
    license.set("LICENSE.txt")
    moduleDescription.set("A module to provide Tag related CICD Endpoints")
    requiredIgnitionVersion.set("8.0.10")

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

    // Optional documentation settings
    // documentationFiles.from(project.file("src/docs/"))
    // documentationIndex.set("index.html")
}

tasks.register("deepClean") {
    dependsOn(allprojects.map { "${it.path}:clean" })
    description = "Executes clean tasks and remove node plugin caches."
    doLast {
        delete(file(".gradle"))
    }
}

tasks.withType<io.ia.sdk.gradle.modl.task.Deploy>().configureEach {
    hostGateway.set(project.property("hostGateway").toString())
}