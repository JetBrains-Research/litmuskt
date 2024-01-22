plugins {
    kotlin("jvm")
    application
}

application {
    mainClass = "MainKt"
}

dependencies {
    implementation(project(":core"))
    implementation(kotlin("reflect"))
}
