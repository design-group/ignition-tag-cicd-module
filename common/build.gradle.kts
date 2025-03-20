plugins {
    `java-library`
}

java {
    toolchain {
        // Set Java version to 17
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)
}