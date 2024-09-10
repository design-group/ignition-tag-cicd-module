plugins {
    `java-library`
}

// Uncomment this block if you need to use the extraLibs configuration
// configurations {
//     create("extraLibs")
// }

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${project.property("sdk_version")}")
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:${project.property("sdk_version")}")
    
    // Any dependencies added to the extraLibs configuration will be included in the jar
    // extraLibs("com.google.code.gson:gson:2.8.6")
    // configurations["compile"].extendsFrom(configurations["extraLibs"])
}

// Uncomment this block to include the extraLibs configuration in the jar
// tasks.jar {
//     from(configurations["extraLibs"].map { if (it.isDirectory) it else zipTree(it) })
// }