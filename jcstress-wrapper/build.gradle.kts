plugins {
    kotlin("jvm")
    application
}

application {
    mainClass = "MainKt"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":testsuite"))
    implementation(kotlin("reflect"))
}

val jcsDir = rootProject.layout.projectDirectory.dir("jcstress")

tasks.register<Copy>("copyCoreToJCStress") {
    dependsOn(":core:jvmJar")
    from(project(":core").layout.buildDirectory.file("libs/core-jvm-$version.jar"))
    rename { "litmusktJvm-1.0.jar" }
    into(jcsDir.dir("libs/org/jetbrains/litmuskt/litmusktJvm/1.0/"))
    doFirst {
        if (inputs.sourceFiles.isEmpty) throw GradleException("missing files to copy")
    }
}

tasks.register<Copy>("copyTestsuiteToJCStress") {
    dependsOn(":testsuite:jvmJar")
    from(project(":testsuite").layout.buildDirectory.file("libs/testsuite-jvm-$version.jar"))
    rename { "litmusktJvmTestsuite-1.0.jar" }
    into(jcsDir.dir("libs/org/jetbrains/litmuskt/litmusktJvmTestsuite/1.0/"))
    doFirst {
        if (inputs.sourceFiles.isEmpty) throw GradleException("missing files to copy")
    }
}
