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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val gitCommitCount: Int = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.get().trim().let {
    10000 + it.toInt()
}

val buildFormatDate: String = DateTimeFormatter.ofPattern("yyMMdd").format(LocalDate.now())

val gitCommitShortId: String = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim()

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

val macPackageVersion: String by lazy {
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
    macPackageVersion = macPackageVersion,
    winUpgradeUuid = winUpgradeUuid,
    programName = programName,
    installProgramName = installProgramName,
    packageVendor = packageVendor,
    copyright = myCopyright,
)

project.extra["appBuildInfo"] = appBuildInfo
