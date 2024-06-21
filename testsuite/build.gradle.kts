plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "1.9.20-1.0.13"
    `java-library`
}

kotlin {
    // targets have to be the same as in :cli (because it depends on this subproject)
    linuxX64()
//    linuxArm64()
    macosX64()
    macosArm64()
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

// ======== ksp ========

val kspTasks = setOf("kspJvm", "kspLinuxX64", "kspMacosX64", "kspMacosArm64")

dependencies {
    for (kspTask in kspTasks) {
        add(kspTask, project(":codegen"))
    }
}
