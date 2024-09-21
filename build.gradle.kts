plugins {
    kotlin("multiplatform") version "2.0.0" apply false
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

subprojects {
    group = "org.jetbrains.litmuskt"
    version = "0.1"
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
