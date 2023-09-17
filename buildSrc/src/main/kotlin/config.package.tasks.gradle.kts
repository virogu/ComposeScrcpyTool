import bean.AppBuildInfo
import java.util.*

plugins {
    id("config.git.version")
}

val appBuildInfo: AppBuildInfo by project

listOf("Msi", "Deb").forEach {
    tasks.create("pack${it}") {
        group = "package"
        dependsOn("package$it")
        doLast {
            val lower = it.lowercase(Locale.getDefault())
            println("do rename task")
            project.rootDir.resolve("out/main/${lower}").listFiles()?.filter { f ->
                f.name.endsWith(".${lower}")
            }?.forEach { f ->
                val newName = "${appBuildInfo.installProgramName}-${appBuildInfo.msiPackageVersion}-" +
                        "${appBuildInfo.gitCommitCount}_${appBuildInfo.gitCommitShortId}.${lower}"
                println("rename [${f.name}] to [$newName]")
                f.renameTo(File(f.parentFile, newName))
            }
        }
    }
}


//task("zipPackageFiles", Zip::class) {
//    rootProject.rootDir.resolve("out/zip").apply {
//        println("clear path:[${this.path}]")
//        this.deleteRecursively()
//    }
//    group = "package"
//    from("C:\\Program Files\\$programName") {
//        //include {
//        //    println("found file [${it.path}]")
//        //    true
//        //}
//    }
//    // programName-myPackageVersion-gitCommitShortid.zip
//    archiveBaseName.set(programName)
//    archiveAppendix.set(msiPackageVersion)
//    archiveVersion.set(gitCommitShortId)
//    archiveExtension.set("zip")
//    destinationDirectory.set(rootProject.rootDir.resolve("out/zip"))
//    doLast {
//        val zipFile = archiveFile.get().asFile
//        val size = DecimalFormat(".##").format(zipFile.length() / (1024 * 1024f))
//        println("zip file [${zipFile.path}] success, size: ${size}MB")
//    }
//}