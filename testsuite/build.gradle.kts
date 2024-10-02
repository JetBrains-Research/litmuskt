import java.net.URI

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    `java-library`
    `maven-publish`
}

kotlin {
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    mingwX64()

    jvm {
        withJava()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
            }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            artifactId = "litmuskt-$artifactId"
        }
    }
    repositories {
        maven {
            url = URI("https://packages.jetbrains.team/maven/p/plan/litmuskt")
            credentials {
                username = System.getenv("SPACE_USERNAME")
                password = System.getenv("SPACE_PASSWORD")
            }
        }
    }
}

// ======== ksp ========

val kspTasks = setOf("kspJvm", "kspLinuxX64", "kspLinuxArm64", "kspMacosX64", "kspMacosArm64", "kspMingwX64")

dependencies {
    for (kspTask in kspTasks) {
        add(kspTask, project(":codegen"))
    }
}
