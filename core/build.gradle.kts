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
            }
        }
    }
    sourceSets {
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
    }
}
