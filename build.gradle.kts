plugins {
    alias(libs.plugins.kotlin.serialization) apply (false)
    alias(libs.plugins.compose) apply (false)
    alias(libs.plugins.gmazzo.buildconfig) apply (false)
}

tasks.register("clean", Delete::class) {
    group = "build"
    delete(rootProject.layout.buildDirectory)
}