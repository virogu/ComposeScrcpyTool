import bean.AppBuildInfo
import java.io.ByteArrayOutputStream
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
        return@with 10000 + revision.toInt()
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
    val MAJOR = (gitCommitCount / 100 / 100) + 1
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

val packageVendor: String by project
val winUpgradeUuid: String by project
val programName: String by project
val installProgramName: String by project
val myCopyright: String by project

val appBuildInfo = AppBuildInfo(
    packageVersion = msiPackageVersion,
    gitCommitShortId = gitCommitShortId,
    buildFormatDate = buildFormatDate,
    gitCommitCount = gitCommitCount,
    packageVersionTriple = packageVersionTriple,
    debPackageVersion = debPackageVersion,
    msiPackageVersion = msiPackageVersion,
    winUpgradeUuid = winUpgradeUuid,
    programName = programName,
    installProgramName = installProgramName,
    packageVendor = packageVendor,
    copyright = myCopyright,
)

project.extra["appBuildInfo"] = appBuildInfo
