plugins {
    kotlin("multiplatform")
    `java-library`
    `maven-publish`
    signing
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
    repositories {
        mavenLocal()
    }
    publications {
        withType<MavenPublication> {
            artifactId = "litmuskt-$artifactId"
        }
    }
}

signing {
    if (hasProperty("enableSigning")) {
        val signingPassword = System.getenv("SIGNING_PASSWORD")
        val signingKey = System.getenv("SIGNING_KEY")

        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
