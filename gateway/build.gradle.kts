plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11  // Ensure this matches the JDK you are using
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // Use `api` if you need the dependencies to be available transitively to other projects,
    // otherwise use `implementation` for better encapsulation and build performance.
    api(project(":common"))

    // `compileOnly` is used for dependencies that are necessary at compile time but not at runtime,
    // such as dependencies provided by the runtime environment.
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${project.property("sdk_version")}")
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:${project.property("sdk_version")}")
}