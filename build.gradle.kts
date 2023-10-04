plugins {
    kotlin("multiplatform") version "1.9.10" apply false
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

subprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
