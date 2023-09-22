tasks.register("clean", Delete::class) {
    group = "build"
    delete(rootProject.buildDir)
}