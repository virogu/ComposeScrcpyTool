import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

val gitCommitCount: Int = with(ByteArrayOutputStream()) {
    use { os ->
        // git rev-list --all --count
        // git rev-list --count HEAD
        exec {
            executable = "git"
            args = listOf("rev-list", "--count", "HEAD")
            standardOutput = os
        }
        val revision = os.toString().trim()
        return@with 30 + revision.toInt()
    }
}
val buildFormatDate: String = with(SimpleDateFormat("yyMMdd")) {
    format(Date())
}

val gitCommitShortId: String = with(ByteArrayOutputStream()) {
    use { os ->
        exec {
            executable = "git"
            args = listOf("rev-parse", "--short", "HEAD")
            standardOutput = os
        }
        return@with os.toString().trim()
    }
}

val packageVersionTriple: Triple<Int, Int, Int> by lazy {
    val MAJOR = (gitCommitCount / 100 / 100).coerceAtLeast(1)
    val MINOR = (gitCommitCount / 100 % 100)
    val PATCH = gitCommitCount % 100
    Triple(MAJOR, MINOR, PATCH)
}

val msiPackageVersion: String by lazy {
    with(packageVersionTriple) {
        "${first}.${second}.${third}"
    }
}

val debPackageVersion: String by lazy {
    with(packageVersionTriple) {
        "${first}.${second}.${third}"
    }
}

val myPackageVendor: String by project
val winUpgradeUuid: String by project
val programName: String by project
val installProgramName: String by project

project.extra["gitCommitCount"] = gitCommitCount
project.extra["buildFormatDate"] = buildFormatDate
project.extra["gitCommitShortId"] = gitCommitShortId
project.extra["packageVersionTriple"] = packageVersionTriple
project.extra["myMsiPackageVersion"] = msiPackageVersion
project.extra["myDebPackageVersion"] = debPackageVersion

tasks.create("packageMsiAndRename") {
    group = "package"
    dependsOn("packageMsi")
    doLast {
        println("do rename task")
        project.rootDir.resolve("out/packages/main/msi").listFiles()?.filter {
            it.name.endsWith(".msi")
        }?.forEach {
            val newName = "$installProgramName-${msiPackageVersion}_${gitCommitShortId}.msi"
            println("rename [${it.name}] to [$newName]")
            it.renameTo(File(it.parentFile, newName))
        }
    }
}

tasks.create("packageDebAndRename") {
    group = "package"
    dependsOn("packageDeb")
    doLast {
        println("do rename task")
        project.rootDir.resolve("out/packages/main/deb").listFiles()?.filter {
            it.name.endsWith(".msi")
        }?.forEach {
            val newName = "$installProgramName-${msiPackageVersion}_${gitCommitShortId}.deb"
            println("rename [${it.name}] to [$newName]")
            it.renameTo(File(it.parentFile, newName))
        }
    }
}

task("zipPackageFiles", Zip::class) {
    rootProject.rootDir.resolve("out/zip").apply {
        println("clear path:[${this.path}]")
        this.deleteRecursively()
    }
    group = "package"
    from("C:\\Program Files\\$programName") {
        //include {
        //    println("found file [${it.path}]")
        //    true
        //}
    }
    // programName-myPackageVersion-gitCommitShortid.zip
    archiveBaseName.set(programName)
    archiveAppendix.set(msiPackageVersion)
    archiveVersion.set(gitCommitShortId)
    archiveExtension.set("zip")
    destinationDirectory.set(rootProject.rootDir.resolve("out/zip"))
    doLast {
        val zipFile = archiveFile.get().asFile
        val size = DecimalFormat(".##").format(zipFile.length() / (1024 * 1024f))
        println("zip file [${zipFile.path}] success, size: ${size}MB")
    }
}