import org.jetbrains.kotlin.incremental.createDirectory

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "1.9.20-1.0.13"
    `java-library`
}

group = "komem.litmus"
version = "1.0-SNAPSHOT"

kotlin {
    val nativeTargets = listOf(
        linuxX64(),
//        linuxArm64(), // 1) no machine currently available 2) CLI library does not support
        macosX64(),
        macosArm64(),
    )

    jvm {
        withSourcesJar()
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
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.20.2")
                implementation("com.github.ajalt.clikt:clikt:4.2.1")
            }
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin/")) // ksp
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
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

dependencies {
    add("kspCommonMainMetadata", project(":codegen"))
}

tasks.whenTaskAdded {
    if (name == "kspCommonMainKotlinMetadata") {
        val kspTask = this
        tasks.matching { it.name.startsWith("compileKotlin") }.forEach { it.dependsOn(kspTask) }
    }
}

tasks.register<Copy>("copyLibToJCStress") {
    dependsOn("jvmJar")
    from(layout.buildDirectory.file("libs/litmus-jvm-$version.jar"))
    into(projectDir.resolve("../jcstress/libs/"))
}
