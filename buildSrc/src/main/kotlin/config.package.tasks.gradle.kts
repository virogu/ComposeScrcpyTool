import bean.AppBuildInfo
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.text.DecimalFormat

plugins {
    id("config.git.version")
}

val appBuildInfo: AppBuildInfo by project

private val outputDir get() = project.rootDir.resolve("out/main")
private val targetPlatform = listOf("msi", "deb")

private fun renameDistribution() {
    targetPlatform.forEach {
        println("rename $it package")
        outputDir.resolve(it).listFiles()?.filter { f ->
            f.isFile && f.name.endsWith(".${it}")
        }?.forEach { f ->
            val newName = with(appBuildInfo) {
                "${installProgramName}-${msiPackageVersion}_${gitCommitShortId}.${f.extension}"
            }
            println("rename [${f.name}] to [$newName]")
            f.renameTo(File(f.parentFile, newName))
        }
    }
}

val pack by tasks.registering {
    group = "package"
    dependsOn("packageDistributionForCurrentOS", packZip)
    doLast {
        renameDistribution()
    }
}

val cleanPackDir by tasks.registering {
    group = "package"
    //dependsOn("clean")
    doLast {
        outputDir.deleteRecursively()
    }
}

val zipDistributable by tasks.registering(Zip::class) {
    group = "package"
    mustRunAfter("createDistributable")
    val path = outputDir.resolve("app/${appBuildInfo.installProgramName}")
    from(path.path)
    with(appBuildInfo) {
        archiveBaseName.set(installProgramName)
        archiveVersion.set("${msiPackageVersion}_$gitCommitShortId")
        archiveExtension.set("zip")
    }
    destinationDirectory.set(outputDir.resolve("zip"))
    doLast {
        val zipFile = archiveFile.get().asFile
        val size = DecimalFormat(".##").format(zipFile.length() / (1024 * 1024f))
        println("zip file [${zipFile.path}] success, size: ${size}MB")
    }
}

targetPlatform.forEach { packName ->
    tasks.create("pack${packName.uppercaseFirstChar()}") {
        group = "package"
        dependsOn("package$packName")
        doLast {
            renameDistribution()
        }
    }
}

val packZip by tasks.registering {
    group = "package"
    dependsOn("createDistributable", zipDistributable)
}
