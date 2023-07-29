import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform") version "1.9.0"
}

group = "komem.litmus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

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
    val jvmTarget = jvm().apply {
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