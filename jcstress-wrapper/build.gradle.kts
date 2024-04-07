plugins {
    kotlin("jvm")
    application
}

application {
    mainClass = "MainKt"
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":testsuite"))
    implementation(kotlin("reflect"))
}

val jcsDir: File get() = File(System.getenv("JCS_DIR") ?: error("JCS_DIR envvar is not set"))

tasks.register<Copy>("copyCoreToJCStress") {
    dependsOn(":core:jvmJar")
    from(project(":core").layout.buildDirectory.file("libs/core-jvm-1.0-SNAPSHOT.jar"))
    if (inputs.sourceFiles.isEmpty) throw BuildCancelledException("missing files to copy")
    rename { "litmusktJvm-1.0.jar" }
    into(jcsDir.resolve("libs/komem/litmus/litmusktJvm/1.0/"))
}

tasks.register<Copy>("copyTestsuiteToJCStress") {
    dependsOn(":testsuite:jvmJar")
    from(project(":testsuite").layout.buildDirectory.file("libs/testsuite-jvm.jar"))
    if (inputs.sourceFiles.isEmpty) throw BuildCancelledException("missing files to copy")
    rename { "litmusktJvmTestsuite-1.0.jar" }
    into(jcsDir.resolve("libs/komem/litmus/litmusktJvmTestsuite/1.0/"))
}
