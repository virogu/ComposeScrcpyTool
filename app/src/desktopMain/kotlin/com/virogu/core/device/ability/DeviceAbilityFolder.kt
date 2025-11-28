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

package com.virogu.core.device.ability

import com.virogu.core.bean.FileVerifyInfo
import com.virogu.core.bean.RemoteFile
import java.io.File

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:35
 **/
interface DeviceAbilityFolder {
    suspend fun remount(): String

    suspend fun refreshPath(parent: RemoteFile, path: String = parent.path): Result<List<RemoteFile>>

    suspend fun createDir(dir: String, newFile: String): Result<String>

    suspend fun createFile(dir: String, newFile: String): Result<String>

    suspend fun deleteFile(path: String): Result<String>

    suspend fun getFileVerifyInfo(path: String): FileVerifyInfo

    suspend fun pullFile(toLocalFile: File, vararg fromRemotePath: String): String

    suspend fun pushFile(toRemotePath: String, vararg fromLocalFiles: File): String

    suspend fun chmod(path: String, permission: String): String
}