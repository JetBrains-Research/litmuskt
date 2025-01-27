plugins {
    kotlin("multiplatform") version "2.1.0" apply false
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

subprojects {
    group = "org.jetbrains.litmuskt"
    version = "0.1.2"
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
