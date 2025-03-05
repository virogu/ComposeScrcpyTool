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
            implementation(compose.ui)
            //implementation(compose.materialIconsExtended)
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
            //val accompanistVersion = "0.30.1"
            //implementation("com.google.accompanist:accompanist-animations:$accompanistVersion")
        }
        commonTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.junit)
            implementation(files("libs\\hdc\\device-file-explorer-4.0.0.600.jar"))
            implementation(files("libs\\hdc\\ohos-hdclib-4.0.0.600.jar"))
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
            modules("java.naming", "jdk.unsupported", "java.rmi", "java.management")
            //includeAllModules = true
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources").also {
                println("resources: ${it.asFile.absolutePath}")
            })
            outputBaseDir.set(project.rootDir.resolve("out"))
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = appBuildInfo.programName
            vendor = appBuildInfo.packageVendor
            copyright = appBuildInfo.copyright
            windows {
                packageVersion = appBuildInfo.msiPackageVersion
                //console = true
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
        }
        //fromFiles(project.fileTree("app/"))
    }
}
