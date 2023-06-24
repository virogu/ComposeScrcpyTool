// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("jvm").version("1.9.20")
        kotlin("plugin.serialization").version("1.9.20")
        id("org.jetbrains.compose").version("1.5.10")
    }

}
rootProject.name = "ComposeScrcpyTool"
