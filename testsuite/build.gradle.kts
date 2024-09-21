plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    `java-library`
    `maven-publish`
    signing
}

kotlin {
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    mingwX64()

    jvm {
        withJava()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
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

// ======== ksp ========

val kspTasks = setOf("kspJvm", "kspLinuxX64", "kspLinuxArm64", "kspMacosX64", "kspMacosArm64", "kspMingwX64")

dependencies {
    for (kspTask in kspTasks) {
        add(kspTask, project(":codegen"))
    }
}
