
dependencyResolutionManagement {
    versionCatalogs { // <1>
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "buildSrc"
