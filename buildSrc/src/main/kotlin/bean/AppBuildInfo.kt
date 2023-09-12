package bean

data class AppBuildInfo(
    val packageVersion: String,
    val gitCommitShortId: String,
    val buildFormatDate: String,
    val gitCommitCount: Int,
    val packageVersionTriple:  Triple<Int, Int, Int>,
    val debPackageVersion: String,
    val msiPackageVersion: String,
    val winUpgradeUuid: String,
    val programName: String,
    val installProgramName: String,
    val packageVendor: String,
    val copyright: String,
)