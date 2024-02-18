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
