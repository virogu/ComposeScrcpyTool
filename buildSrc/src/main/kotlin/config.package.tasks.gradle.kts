/*
 * Copyright 2024 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
    val pack = packName.uppercaseFirstChar()
    tasks.create("pack${pack}") {
        group = "package"
        dependsOn("package$pack")
        doLast {
            renameDistribution()
        }
    }
}

val packZip by tasks.registering {
    group = "package"
    dependsOn("createDistributable", zipDistributable)
}
