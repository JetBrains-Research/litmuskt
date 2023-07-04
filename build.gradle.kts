plugins {
    kotlin("multiplatform") version "1.9.0-RC"
}

group = "me.denis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val armEnabled = findProperty("arm") != null

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> if (armEnabled) macosArm64("macos") else macosX64("macos")
        hostOs == "Linux" -> linuxX64("linux")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
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
                implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0-RC")
                implementation("org.jetbrains.kotlinx:atomicfu:0.20.2")
            }
        }
        val commonTest by getting

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

            isMingwX64 -> {
                throw GradleException("Windows is not yet supported in this project")
            }
        }
    }
}
