# Detailed Guide: Building the Example Component Library Module

This guide provides an in-depth explanation of how the Example Component Library module is built using Gradle, the Ignition plugin, NPM, and how the various project components are integrated.

## Overview of the Build Process

The build process for the Example Component Library module involves several steps:

1. Compiling Java code for the common, gateway, and designer projects
2. Packaging everything into a `.modl` file

Let's dive into each of these steps and the tools involved.

## Gradle and the Ignition Plugin

### Gradle

Gradle is the build automation tool used in this project. It manages dependencies, compiles code, runs tests, and packages the module.

The main Gradle files in the project are:

- `build.gradle.kts`: The root project build file, this defines the Ignition module details that will be created, and the included subprojects
- `settings.gradle.kts`: Defines the project structure and links to the IA module building plugin
- `gradle.properties`: Contains project-wide properties
- `common/build.gradle.kts`, `designer/build.gradle.kts`, `gateway/build.gradle.kts`, `web/build.gradle.kts`: Subproject-specific build files

The repository comes with a `gradle.properties.template` that can be used to create a `gradle.properties` file with the necessary properties for the project. Helpful comments are included to explain the purpose of each property.

## Version Catalog and Dependency Management

This project uses Gradle's version catalog feature, which is defined in the `gradle/libs.versions.toml` file. This file centrally manages dependencies and their versions across the entire project.

### Using the Version Catalog

In the build.gradle.kts files, dependencies from the version catalog are referenced using the libs accessor:

```kotlin
dependencies {
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.perspective.common)
    compileOnly(libs.google.guava)
}
```

This approach centralizes dependency management and makes it easier to maintain consistent versions across modules.

### Ignition Module Plugin

The Ignition Module Plugin is a custom Gradle plugin that provides tasks specific to building Ignition modules. It's applied in the root `build.gradle.kts` file:

```kotlin
plugins {
    id("io.ia.sdk.modl") version("0.3.0")
}
```

This plugin provides tasks like `ignitionModule` and `deployModl`. This is what will package the module into a `.modl` file and allow us to auto-deploy it to an Ignition Gateway.

## Project Structure and Dependencies

The project is structured into several subprojects, which are not specifically named and can be customized to fit your needs. A common structure is:

- `common`: Shared code for both Gateway and Designer
  - This is optional for code organization, but can be useful for sharing code between the different module scopes.
- `gateway`: Gateway-specific code
  - The SDK actually cares about this scope, and is treated as the "code that runs in the gateway"

### Subproject Dependencies

The dependencies between subprojects are defined in their respective `build.gradle.kts` files. For example, in `gateway/build.gradle.kts`:

```kotlin
dependencies {
	// This allows the code built in the :common project to be used in the :gateway project
    api(projects.common)
	
	// This is required to get the :web project .jar added into the modl file
    modlImplementation(projects.web)

    // Other dependencies...
}
```

This tells Gradle that the `gateway` project depends on the `common` project, and should include the files from the `web` project in the final module.


### Gradle Integration

The `web/build.gradle.kts` file includes tasks to run npm and Webpack:

```kotlin
val installDependencies by tasks.registering(NpmTask::class) {
    args.set(listOf("install"))
}

val webpack by tasks.registering(NpmTask::class) {
    args.set(listOf("run", "build"))

    dependsOn(installDependencies)
    
    inputs.files(project.fileTree(".").matching {
        exclude("**/node_modules/**", "**/dist/**", "**/.awcache/**", "**/yarn-error.log")
    }.toList())

    outputs.files(fileTree(projectOutput))
}
```

These tasks are made dependencies of the `:gateway` project's `build` task through the `processResources` task:

```kotlin
project(":gateway")?.tasks?.named("processResources")?.configure {
    dependsOn(webpack)
}
```

### Resource Copying

After the web resources are built, they need to be copied to a location where the `:gateway` project can find them. This is handled by setting out `output` directory in the `webpack` task.

```kotlin

val webpack by tasks.registering(NpmTask::class) {
    // Build the files here

    outputs.files(fileTree(projectOutput))
}
```

This task copies the compiled web resources to the `gateway` project's resources directory.

## Building the Module

When you run `./gradlew build`, Gradle performs these steps:

1. Compiles Java code for `common`, and `gateway` projects
2. Copies web resources to the `gateway` project
4. Packages everything into a `.modl` file using the Ignition Module Plugin

The resulting `.modl` file will be in the root `build/` directory.

## Best Practices and Tips

1. Always run a clean build when making significant changes: `./gradlew clean build`
2. Use the `--info` or `--debug` flag with Gradle commands for more detailed output if you're troubleshooting build issues
3. Keep your `gradle.properties` file up to date, especially when working with different Ignition versions or Gateway URLs
4. Regularly update the Ignition SDK and Module Plugin versions to stay current with the latest features and bug fixes

## Troubleshooting Common Issues

- If the module fails to load in Ignition, check the Gateway logs for detailed error messages
- For web component issues, check the browser console and the compiled JavaScript in the `.modl` file
- If the module fails to deploy to the gateway with the following error, you may need to add your CA Certs to your java keystore: `sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target`. This error is because the certificate authority that you used to define your certs isnt trusted by default in Java. To fix the error, adding the CA cert and intermediates to your java keystore will allow the module to deploy.

Example Commands to add your CA Certs to your java keystore:
```
keytool -import -cacerts -alias root_ca -file root.crt -storepass changeit 
keytool -import -cacerts -alias localtest -file localtest.crt -storepass changeit
```

Remember, the build process integrates multiple technologies and steps. Understanding each part of this process will help you troubleshoot issues and extend the module effectively.