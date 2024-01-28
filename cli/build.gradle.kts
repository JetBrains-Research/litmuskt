plugins {
    kotlin("multiplatform")
}

kotlin {
    val nativeTargets = listOf(
        linuxX64(),
//        linuxArm64(), // 1) no machine currently available 2) CLI library does not support
        macosX64(),
        macosArm64(),
    )
    nativeTargets.forEach { target ->
        target.binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    jvm {
        withJava()
        jvmToolchain(8)
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(project(":testsuite"))
                implementation("com.github.ajalt.clikt:clikt:4.2.1")
            }
        }
        jvmMain {
            dependencies {
                implementation(project(":jcstress-wrapper"))
            }
        }
    }
}

val jcsDir: File get() = File(System.getenv("JCS_DIR") ?: error("JCS_DIR envvar is not set"))

tasks.whenTaskAdded {
    if (name == "jvmRun") {
        dependsOn(":core:copyLibToJCStress")
        dependsOn(":jcstress-wrapper:run")
    }
}
