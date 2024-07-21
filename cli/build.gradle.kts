plugins {
    kotlin("multiplatform")
}

kotlin {
    val nativeTargets = listOf(
        linuxX64(),
        // 1) no machine currently available 2) CLI library does not support
//        linuxArm64(),
        macosX64(),
        macosArm64(),
        mingwX64(),
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
    }

    sourceSets {
        commonMain {
            val cliktVersion = project.findProperty("cliktVersion")
            dependencies {
                implementation(project(":core"))
                implementation(project(":testsuite"))
                implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
            }
        }
        jvmMain {
            dependencies {
                implementation(project(":jcstress-wrapper"))
            }
        }
    }
}

tasks.whenTaskAdded {
    if (name == "jvmRun") {
        dependsOn(":jcstress-wrapper:copyCoreToJCStress")
        dependsOn(":jcstress-wrapper:copyTestsuiteToJCStress")
        dependsOn(":jcstress-wrapper:run")
    }
}
