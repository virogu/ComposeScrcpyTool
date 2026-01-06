/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import bean.AppBuildInfo
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("config.package.tasks")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gmazzo.buildconfig)
    //alias(libs.plugins.ksp)
}

val appBuildInfo: AppBuildInfo by project

kotlin {
    jvm("desktop")
    sourceSets {
        val desktopMain by getting
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            //implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.compose)

            // https://mvnrepository.com/artifact/org.apache.sshd/sshd-mina
            // https://github.com/apache/mina-sshd/blob/master/docs/client-setup.md
            implementation(libs.apache.sshd.core)
            implementation(libs.apache.sshd.mina)
            implementation(libs.apache.sshd.common)
            implementation(libs.apache.sshd.putty)

            //implementation(libs.slf4j.api)
            //implementation(libs.logback.core)
            implementation(libs.logback.classic)
            implementation(libs.kotlin.logging)

            // https://github.com/Kodein-Framework/Kodein-DI
            implementation(libs.kodein.di.jvm)
            implementation(libs.kodein.di.conf.jvm)

            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            // https://developer.android.com/jetpack/androidx/releases/datastore?hl=zh-cn
            implementation(libs.datastore.preferences.core)

            //https://google.github.io/accompanist/
        }
        commonTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.junit)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

//dependencies {
//    add("kspCommonMainMetadata", project(":app"))
//}

buildConfig {
    className("BuildConfig")   // forces the class name. Defaults to 'BuildConfig'
    packageName("tools")
    //useJavaOutput()                                 // forces the outputType to 'java'
    useKotlinOutput()                               // forces the outputType to 'kotlin', generating an `object`
    //useKotlinOutput { topLevelConstants = true }    // forces the outputType to 'kotlin', generating top-level declarations
    //useKotlinOutput { internalVisibility = true }   // adds `internal` modifier to all declarations

    buildConfigField("String", "AppName", "\"${appBuildInfo.programName}\"")
    buildConfigField("String", "AppVersion", provider { "\"${appBuildInfo.packageVersion}\"" })
    buildConfigField("String", "WinUpgradeUid", "\"${appBuildInfo.winUpgradeUuid}\"")
    buildConfigField("String", "GitCommitCount", "\"${appBuildInfo.gitCommitCount}\"")
    buildConfigField("String", "GitCommitShortId", "\"${appBuildInfo.gitCommitShortId}\"")
    buildConfigField("String", "BuildVersion", "\"${appBuildInfo.packageVersion}\"")
    buildConfigField("String", "BuildTime", "\"${appBuildInfo.buildFormatDate}\"")
    // buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
    // buildConfigField("boolean", "FEATURE_ENABLED", "${true}")
    // buildConfigField("IntArray", "MAGIC_NUMBERS", "intArrayOf(1, 2, 3, 4)")
    // buildConfigField("com.github.gmazzo.SomeData", "MY_DATA", "new SomeData(\"a\",1)")
}

composeCompiler {

}

compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs += listOf(
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-Dfile.encoding=GBK",
            "-XX:+UseParallelGC"
        )
        //args += listOf("-customArgument")
        nativeDistributions {
            buildTypes.release.proguard {
                configurationFiles.from(rootProject.rootDir.resolve("proguard-rules.pro"))
                obfuscate.set(false)
                optimize.set(true)
            }
            //./gradlew :app:suggestModules
            modules(
                "java.instrument",
                "java.management",
                "java.naming",
                "java.rmi",
                "java.security.jgss",
                "jdk.unsupported"
            )
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources").also {
                println("resources: ${it.asFile.absolutePath}")
            })
            outputBaseDir.set(project.rootDir.resolve("out"))
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = appBuildInfo.programName
            vendor = appBuildInfo.packageVendor
            copyright = appBuildInfo.copyright
            windows {
                packageVersion = appBuildInfo.msiPackageVersion
                console = true
                menu = true
                dirChooser = true
                shortcut = true
                perUserInstall = false
                //menuGroup = "Tools"
                iconFile.set(project.file("logo/logo.ico"))
                upgradeUuid = appBuildInfo.winUpgradeUuid
            }
            linux {
                iconFile.set(project.file("logo/logo.png"))
                packageVersion = appBuildInfo.debPackageVersion
                // an email of the deb package's maintainer;
                debMaintainer = "virogu@foxmail.com"
                // a menu group for the application;
                menuGroup = "Development"
                // a release value for the rpm package, or a revision value for the deb package;
                appRelease = "${appBuildInfo.gitCommitCount}"
                // a group value for the rpm package, or a section value for the deb package;
                appCategory = "utils"
                //installationPath = "/data/opt/apps"
                shortcut = true
            }
            macOS {
                bundleID = "com.virogu.compose.scrcpy"
                appCategory = "public.app-category.developer-tools"
                iconFile.set(project.file("logo/logo.icns"))
                packageVersion = appBuildInfo.macPackageVersion
                packageBuildVersion = "${appBuildInfo.gitCommitCount}"
                dockName = appBuildInfo.programName
            }
        }
        //fromFiles(project.fileTree("app/"))
    }
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}