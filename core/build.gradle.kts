plugins {
    kotlin("multiplatform")
    `java-library`
}

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
        jvmToolchain(17)
    }

    val hostOs = System.getProperty("os.name")
    val affinitySupported = hostOs == "Linux"
    nativeTargets.forEach { target ->
        target.apply {
            compilations.getByName("main") {
                cinterops {
                    create("barrier") {
                        defFile(project.file("src/nativeInterop/barrier.def"))
                        headers(project.file("src/nativeInterop/barrier.h"))
                    }
                    if (affinitySupported) {
                        create("affinity") {
                            defFile(project.file("src/nativeInterop/kaffinity.def"))
                            compilerOpts.add("-D_GNU_SOURCE")
                        }
                    }
                    create("kpthread") {
                        defFile(project.file("src/nativeInterop/kpthread.def"))
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
    }
}
