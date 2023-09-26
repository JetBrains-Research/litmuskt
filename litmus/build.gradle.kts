import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.incremental.createDirectory

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "1.9.10-1.0.13"
}

group = "komem.litmus"
version = "1.0-SNAPSHOT"

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default {
        common {
            withJvm()
            withNative()
            withLinux()
            withMacos()
        }
    }

    val armEnabled = findProperty("arm") != null
    val hostOs = System.getProperty("os.name")
//    val isMingwX64 = hostOs.startsWith("Windows")

    val nativeTarget = when {
        hostOs == "Mac OS X" -> if (armEnabled) macosArm64() else macosX64()
        hostOs == "Linux" -> linuxX64()
        else -> throw GradleException("Host OS is not supported")
    }
    val jvmTarget = jvm {
        // executable by default
        mainRun {
            mainClass.set("JvmMainKt")
        }
    }

    val affinitySupported = hostOs == "Linux"
    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val barrier by creating {
                    defFile(project.file("src/nativeInterop/barrier.def"))
                    headers(project.file("src/nativeInterop/barrier.h"))
                }
                if (affinitySupported) {
                    val affinity by creating {
                        defFile(project.file("src/nativeInterop/kaffinity.def"))
                        headers(project.file("src/nativeInterop/kaffinity.h"))
                    }
                }
            }
            if (gradle.startParameter.taskNames.any { it.contains("bitcode") }) {
                val tempDir = projectDir.resolve("temp/bitcode")
                if (!tempDir.exists()) tempDir.createDirectory()
                kotlinOptions.freeCompilerArgs = listOf("-Xtemporary-files-dir=${tempDir.absolutePath}")
            }
        }
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.20.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
            }
        }

        val nativeMain by getting
        val nativeTest by getting

        val jvmMain by getting
        val jvmTest by getting

        when {
            hostOs == "Mac OS X" -> {
                val macosMain by getting {
                    dependsOn(commonMain)
                    kotlin.srcDirs("src/macosMain/kotlin")
                }
            }

            hostOs == "Linux" -> {
                val linuxMain by getting {
                    dependsOn(commonMain)
                    kotlin.srcDirs("src/linuxMain/kotlin")
                }
            }
        }
    }
}

val setupCinterop by tasks.register("setupCinterop") {
    group = "interop"
    doFirst {
        val interopFolder = project.projectDir.resolve("src/nativeInterop")
        if (!interopFolder.resolve("kaffinity.def").exists()) {
            exec {
                executable = interopFolder.resolve("setup.sh").absolutePath
                args = listOf(interopFolder.absolutePath)
            }
        }
    }
}

tasks.matching { it.name.contains("cinterop") && it.name.contains("Linux") }
    .forEach { it.dependsOn(setupCinterop) }

val bitcodeInternal by tasks.register("bitcodeInternal") {
    val tempDir = projectDir.resolve("temp/bitcode")
    doLast {
        exec {
            executable = "sh"
            args = listOf(
                "-c", """
                llvm-dis -o ${tempDir.resolve("bitcode.txt")} ${tempDir.resolve("out.bc")}
            """.trimIndent()
            )
        }
    }
}

tasks.register("bitcodeDebug") {
    dependsOn(tasks.matching { it.name.startsWith("linkDebugExecutable") })
    finalizedBy(bitcodeInternal)
}

tasks.register("bitcodeRelease") {
    dependsOn(tasks.matching { it.name.startsWith("linkReleaseExecutable") })
    finalizedBy(bitcodeInternal)
}

// ======== ksp ========

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")
}

dependencies {
    add("kspCommonMainMetadata", project(":ksp"))
}
