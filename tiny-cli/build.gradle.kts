plugins {
    id("com.github.minigdx.gradle.plugin.developer.mpp") version "DEV-SNAPSHOT"
    kotlin("plugin.serialization") version "1.8.0"
    application
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.danielgergely.com/releases/")
    }
}

dependencies {
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    commonMainImplementation("com.github.ajalt.clikt:clikt:3.5.2")

    jvmMainImplementation(project(":tiny-engine", "jvmRuntimeElements"))!!
        .because("Depends on the JVM Jar containing commons resources in the JAR.")
    jvmMainImplementation("com.danielgergely.kgl:kgl-lwjgl:0.6.1")
}

application {
    mainClass.set("com.github.minigdx.tiny.cli.MainKt")
    applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")

    // Copy the JARs from the Kotlin MPP dependencies.
    this.applicationDistribution.from(
        project.configurations.getByName("jvmRuntimeClasspath")
    ) {
        val jvmJar by tasks.existing
        this.from(jvmJar)
        this.into("lib")
    }
}

// Update the start script to include jar from the Kotlin MPP dependencies
project.tasks.withType(CreateStartScripts::class.java).configureEach {
    this.classpath = project.tasks.getByName("jvmJar").outputs.files
        .plus(project.configurations.getByName("jvmRuntimeClasspath"))
}

// Make the application plugin start with the right classpath
// See https://youtrack.jetbrains.com/issue/KT-50227/MPP-JVM-target-executable-application
project.tasks.withType(JavaExec::class.java).configureEach {
    val jvmJar by tasks.existing
    val jvmRuntimeClasspath by configurations.existing

    classpath(jvmJar, jvmRuntimeClasspath)
}
