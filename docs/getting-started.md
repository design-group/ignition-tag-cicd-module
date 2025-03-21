# Getting Started Guide

Welcome to the Example Component Library project! This guide will help you get up and running with development. We'll cover the prerequisites, setup process, and provide resources to help you understand the technologies we're using.

## Prerequisites

Before you begin, make sure you have the following tools installed:

- Java 17
- Gradle 7.6
- Node.js (LTS version)
- npm (comes with Node.js)
- Docker and Docker Compose
- VS Code (recommended IDE)

If you haven't set up your development environment yet, please follow our [Environment Setup Guide](environment-setup.md) for detailed instructions.

## Confirming Your Setup

After following the environment setup guide, verify your installation by running these commands in your terminal:

```bash
java -version
gradle -version
node -v
npm -v
docker --version
docker-compose --version
```

Each command should return version information without errors. If you encounter any issues, refer to the environment setup guide or reach out to the team for assistance.

## Project Setup

1. Confirm that you can build the project by running the following command:
   ```bash
   ./gradlew clean build
   ```
2. If the build is successful, you're ready to start developing!
3. If the build fails, first confirm your environment setup and dependencies. If you're still having issues, reach out to the team for help. (Make sure to create any issues on the repository for feedback on the setup requirements!)

## Understanding the Technologies

If you're new to any of the technologies we're using, here are some great resources to get you started:

### Java
- [Java Tutorial for Beginners](https://www.programiz.com/java-programming)
- [Java Documentation](https://docs.oracle.com/en/java/)

### Gradle
- [Gradle Getting Started Guide](https://docs.gradle.org/current/userguide/getting_started.html)
- [Gradle Java Plugin Tutorial](https://docs.gradle.org/current/userguide/building_java_projects.html)

### Kotlin DSL for Gradle
- [Gradle Kotlin DSL Primer](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Migrating build logic from Groovy to Kotlin](https://docs.gradle.org/current/userguide/migrating_from_groovy_to_kotlin_dsl.html)
- [Gradle-VS-Kotlin](./faq/groovy-vs-kotlin.md)

## Ignition SDK and Module Development

To understand how Ignition modules work and how to develop them:

1. [Ignition SDK Documentation](https://www.sdk-docs.inductiveautomation.com/docs/intro/)
2. [Ignition SDK JavaDocs](https://github.com/inductiveautomation/ignition-sdk-examples/wiki/Javadocs-&-Notable-API-Changes)
3. [Ignition Module Examples](https://github.com/inductiveautomation/ignition-sdk-examples)
4. [Ignition Forums Module Development Category](https://forum.inductiveautomation.com/c/module-development/7)

## Project Structure

Our project is structured as follows:

- `common/`: Shared code for both Gateway and Designer
- `gateway/`: Gateway-specific code
- `docker/`: Docker-related files for development and testing
- `gradle/`: Contains the `libs.versions.toml` file for centralized dependency management

### Gradle Build Scripts

This project uses Kotlin DSL for Gradle build scripts. You'll find `build.gradle.kts` files instead of `build.gradle` files. Kotlin DSL provides better IDE support, type safety, and consistency with Kotlin projects.

Key Gradle files:
- `build.gradle.kts`: Root project build file
- `settings.gradle.kts`: Defines the project structure and includes subprojects
- `gradle.properties`: Contains project-wide properties
- `gradle/libs.versions.toml`: Centralizes dependency management (version catalog)

## Building the Module

Our project uses Gradle for building. The main tasks you'll use are:

- `./gradlew clean build`: Cleans and builds the entire project

The build process:
1. Compiles Java code
2. Builds web resources (TypeScript/React components)
3. Packages everything into a `.modl` file


## Next Steps

Now that you're set up and familiar with the basics, try building the project and deploying it to your local Ignition gateway. Then extend functionality, fix bugs, or add new features to the module.

If you run into any issues or have questions, don't hesitate to reach out to the team or consult the project documentation.
