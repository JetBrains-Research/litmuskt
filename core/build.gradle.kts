import java.net.URI

plugins {
    kotlin("multiplatform")
    `java-library`
    `maven-publish`
}

kotlin {
    val nativeTargets = listOf(
        linuxX64(),
        linuxArm64(),
        macosX64(),
        macosArm64(),
        mingwX64(),
    )

    jvm {
        withJava()
    }

    nativeTargets.forEach { target ->
        target.apply {
            compilations.getByName("main") {
                cinterops {
                    create("barrier") {
                        defFile(project.file("src/nativeInterop/barrier.def"))
                        headers(project.file("src/nativeInterop/barrier.h"))
                        compilerOpts.addAll(listOf("-Wall", "-Werror"))
                    }
                    val affinitySupported = target.name.startsWith("linux")
                    if (affinitySupported) {
                        create("affinity") {
                            defFile(project.file("src/nativeInterop/kaffinity.def"))
                            compilerOpts.add("-D_GNU_SOURCE")
                            compilerOpts.addAll(listOf("-Wall", "-Werror"))
                        }
                    }
                    create("kpthread") {
                        defFile(project.file("src/nativeInterop/kpthread.def"))
                        compilerOpts.addAll(listOf("-Wall", "-Werror"))
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

publishing {
    publications {
        withType<MavenPublication> {
            artifactId = "litmuskt-$artifactId"
        }
    }
    repositories {
        maven {
            url = URI("https://packages.jetbrains.team/maven/p/plan/litmuskt")
            credentials {
                username = System.getenv("SPACE_USERNAME")
                password = System.getenv("SPACE_PASSWORD")
            }
        }
    }
}
