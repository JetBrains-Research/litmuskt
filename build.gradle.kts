plugins {
    kotlin("multiplatform") version "1.9.20" apply false
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

subprojects {
    group = "org.jetbrains.litmuskt"
    version = "1.0-SNAPSHOT"
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
