import org.jetbrains.kotlin.incremental.createDirectory

plugins {
    kotlin("multiplatform")
    `java-library`
}

group = "komem.litmus"
version = "1.0-SNAPSHOT"

kotlin {
    val nativeTargets = listOf(
        linuxX64(),
        // 1) no machine currently available 2) CLI library does not support
//        linuxArm64(),
        macosX64(),
        macosArm64(),
    )

    jvm {
        withJava()
        jvmToolchain(8)
    }

    val hostOs = System.getProperty("os.name")
    val affinitySupported = hostOs == "Linux"
    nativeTargets.forEach { target ->
        target.apply {
            compilations.getByName("main") {
                cinterops {
                    val barrier by creating {
                        defFile(project.file("src/nativeInterop/barrier.def"))
                        headers(project.file("src/nativeInterop/barrier.h"))
                    }
                    if (affinitySupported) {
                        val affinity by creating {
                            defFile(project.file("src/nativeInterop/kaffinity.def"))
                            compilerOpts.add("-D_GNU_SOURCE")
                        }
                    }
                }
                if (gradle.startParameter.taskNames.any { it.contains("bitcode") }) {
                    val tempDir = projectDir.resolve("temp/bitcode")
                    if (!tempDir.exists()) tempDir.createDirectory()
                    kotlinOptions.freeCompilerArgs = listOf("-Xtemporary-files-dir=${tempDir.absolutePath}")
                }
            }
        }
    }
    sourceSets {
        commonMain {
            val atomicfuVersion = project.findProperty("atomicfuVersion")
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        jvmMain {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }

        macosMain {
            kotlin.srcDirs("src/macosMain/kotlin")
        }

        linuxMain {
            kotlin.srcDirs("src/linuxMain/kotlin")
        }
    }
}

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

val jcsDir: File get() = File(System.getenv("JCS_DIR") ?: error("JCS_DIR envvar is not set"))

tasks.register<Copy>("copyLibToJCStress") {
    dependsOn("jvmJar")
    from(layout.buildDirectory.file("libs/core-jvm-$version.jar"))
    rename { "litmusktJvm-1.0.jar" }
    into(jcsDir.resolve("libs/komem/litmus/litmusktJvm/1.0/"))
}
