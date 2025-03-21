plugins {
    `java-library`
}

// Configure Java compilation settings
java {
    toolchain {
        // Set Java version to 17
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


dependencies {
     // Regular implementation dependency - included in the final jar
    implementation(projects.common)

    // Runtime dependencies provided by Ignition
    compileOnly(libs.ignition.common)  // Core Ignition classes
    compileOnly(libs.ignition.gateway.api)  // Gateway-specific API
}