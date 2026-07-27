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
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.text.DecimalFormat

plugins {
    id("config.git.version")
}

val appBuildInfo = extra["appBuildInfo"] as AppBuildInfo

private val outputDir get() = project.rootDir.resolve("out/main-release")
private val targetPlatform = listOf("msi", "deb", "dmg")

private fun renameDistribution() {
    targetPlatform.forEach {
        logger.lifecycle("rename $it package")
        outputDir.resolve(it).listFiles()?.filter { f ->
            f.isFile && f.name.endsWith(".${it}")
        }?.forEach { f ->
            val newName = with(appBuildInfo) {
                "${installProgramName}-${msiPackageVersion}_${gitCommitShortId}.${f.extension}"
            }
            logger.lifecycle("rename [${f.name}] to [$newName]")
            f.renameTo(File(f.parentFile, newName))
        }
    }
}

val packZip = tasks.register("packZip") {
    description = "packZip"
    group = "package"
    dependsOn("createReleaseDistributable", "zipDistributable")
}

val pack = tasks.register("pack") {
    description = "packAll"
    group = "package"
    dependsOn("packageReleaseDistributionForCurrentOS", packZip)
    doLast {
        renameDistribution()
    }
}

val cleanPackDir = tasks.register("cleanPackDir") {
    description = "cleanPackDir"
    group = "package"
    doLast {
        outputDir.deleteRecursively()
    }
}

val zipDistributable = tasks.register<Zip>("zipDistributable") {
    description = "zipDistributable"
    group = "package"
    mustRunAfter("createReleaseDistributable")
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
        logger.lifecycle("zip file [${zipFile.path}] success, size: ${size}MB")
    }
}

targetPlatform.forEach { packName ->
    val packNameCap = packName.uppercaseFirstChar()
    tasks.register("pack${packNameCap}") {
        description = "pack${packNameCap}"
        group = "package"
        dependsOn("packageRelease$packNameCap")
        doLast {
            renameDistribution()
        }
    }
}
