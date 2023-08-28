import org.jetbrains.compose.desktop.application.dsl.TargetFormat

apply("config.gradle.kts")

val kotlinVersion = "1.9.20"
val programName: String by project
val gitCommitCount: Int by project
val buildFormatDate: String by project
val gitCommitShortId: String by project
val myPackageVersion: String by project
val myPackageVendor: String by project
val myCopyright: String by project
val winUpgradeUuid: String by project
val javaVersion = JavaVersion.VERSION_17
val javaVersionString = "17"

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("com.github.gmazzo.buildconfig") version "3.0.3"
    kotlin("plugin.serialization")
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = javaVersionString
    }
}
tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = javaVersionString
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    //implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.0-native-mt")
    //implementation("com.jakewharton.timber:timber:4.7.1")
    // https://mvnrepository.com/artifact/org.apache.sshd/sshd-mina
    // https://github.com/apache/mina-sshd/blob/master/docs/client-setup.md

    val sshdVersion = "2.10.0"
    implementation("org.apache.sshd:sshd-mina:$sshdVersion")
    implementation("org.apache.sshd:sshd-core:$sshdVersion")
    implementation("org.apache.sshd:sshd-common:$sshdVersion")
    implementation("org.apache.sshd:sshd-putty:$sshdVersion")
    //implementation("org.apache.sshd:apache-sshd:$sshdVersion")
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-core:1.4.8")
    implementation("ch.qos.logback:logback-classic:1.4.8")

    // https://github.com/Kodein-Framework/Kodein-DI
    val diVersion = "7.14.0"
    implementation("org.kodein.di:kodein-di-jvm:${diVersion}")
    implementation("org.kodein.di:kodein-di-conf-jvm:${diVersion}")
    //implementation("org.kodein.di:kodein-di-framework-android-x:${diVersion}")

    val serializableVersion = "1.5.0"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializableVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializableVersion")
    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$serializableVersion")

    //implementation("androidx.datastore:datastore-core:1.0.0")
    implementation("androidx.datastore:datastore-preferences-core:1.1.0-alpha01")

    //https://github.com/russhwolf/multiplatform-settings
    //implementation("com.russhwolf:multiplatform-settings-datastore:1.0.0")
    //implementation("com.russhwolf:multiplatform-settings-serialization:1.0.0")

    //https://google.github.io/accompanist/
    //val accompanistVersion = "0.30.1"
    //implementation("com.google.accompanist:accompanist-animations:$accompanistVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

buildConfig {
    className("BuildConfig")   // forces the class name. Defaults to 'BuildConfig'
    packageName("tools")
    //useJavaOutput()                                 // forces the outputType to 'java'
    useKotlinOutput()                               // forces the outputType to 'kotlin', generating an `object`
    //useKotlinOutput { topLevelConstants = true }    // forces the outputType to 'kotlin', generating top-level declarations
    //useKotlinOutput { internalVisibility = true }   // adds `internal` modifier to all declarations

    buildConfigField("String", "AppName", "\"${programName}\"")
    buildConfigField("String", "AppVersion", provider { "\"${myPackageVersion}\"" })
    buildConfigField("String", "WinUpgradeUid", "\"$winUpgradeUuid\"")
    buildConfigField("String", "GitCommitCount", "\"${gitCommitCount}\"")
    buildConfigField("String", "GitCommitShortId", "\"${gitCommitShortId}\"")
    buildConfigField("String", "BuildVersion", "\"$myPackageVersion\"")
    buildConfigField("String", "BuildTime", "\"${buildFormatDate}\"")
    // buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
    // buildConfigField("boolean", "FEATURE_ENABLED", "${true}")
    // buildConfigField("IntArray", "MAGIC_NUMBERS", "intArrayOf(1, 2, 3, 4)")
    // buildConfigField("com.github.gmazzo.SomeData", "MY_DATA", "new SomeData(\"a\",1)")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            modules(
                "java.naming", "jdk.unsupported",
                "java.rmi", "java.management",
            )
            //includeAllModules = true
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources").also {
                println("resources: ${it.asFile.absolutePath}")
            })
            outputBaseDir.set(project.rootDir.resolve("out/packages"))
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = programName
            vendor = myPackageVendor
            copyright = myCopyright
            windows {
                packageVersion = myPackageVersion
                //console = true
                menu = true
                dirChooser = true
                shortcut = true
                perUserInstall = false
                //menuGroup = "Tools"
                iconFile.set(project.file("logo/logo.ico"))
                upgradeUuid = winUpgradeUuid
            }
            linux {
                iconFile.set(project.file("logo/logo.png"))
                packageVersion = myPackageVersion
                // an email of the deb package's maintainer;
                debMaintainer = "virogu@foxmail.com"
                // a menu group for the application;
                menuGroup = "Development"
                // a release value for the rpm package, or a revision value for the deb package;
                appRelease = "$gitCommitCount"
                // a group value for the rpm package, or a section value for the deb package;
                appCategory = "utils"
                //installationPath = "/data/opt/apps"
                shortcut = true
            }
        }
        //fromFiles(project.fileTree("app/"))
    }
}
