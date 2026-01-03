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

package com.virogu.core.viewmodel

import androidx.lifecycle.viewModelScope
import com.virogu.core.bean.RemoteFile
import com.virogu.core.bean.RemoteFileLoadState
import com.virogu.core.device.Device
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.io.File

class RemoteFileViewModel : BaseJobViewModel() {
    private val _tipsFlow = MutableSharedFlow<String>()
    val tipsFlow: SharedFlow<String> get() = _tipsFlow

    fun emitTips(tips: String) {
        viewModelScope.launch {
            _tipsFlow.emit(tips)
        }
    }

    private suspend fun emitTipsFlow(tips: String) {
        if (tips.isEmpty()) {
            return
        }
        _tipsFlow.emit(tips)
    }

    fun refresh(file: RemoteFile) {
        if (file.isDirectory) {
            file.children.value = emptyList()
            file.childrenLoadStates.value = RemoteFileLoadState.NotLoad
        } else {
            file.parent?.also(::refresh)
        }
    }

    fun pullFile(device: Device, toLocalFile: File, vararg fromRemotePath: String) {
        viewModelScope.launch {
            if (!toLocalFile.isDirectory) {
                emitTipsFlow("[${toLocalFile.absolutePath}]不是目录")
                return@launch
            }
            if (!toLocalFile.exists()) {
                emitTipsFlow("[${toLocalFile.absolutePath}]不存在")
                return@launch
            }
            lineJob {
                device.folderAbility.pullFile(toLocalFile, *fromRemotePath)
            }.also {
                emitTipsFlow(it)
            }
        }
    }

    fun pushFile(device: Device, toRemoteFile: RemoteFile, vararg fromLocalFiles: File) {
        viewModelScope.launch {
            if (!toRemoteFile.isDirectory) {
                emitTipsFlow("[${toRemoteFile.path}]不是目录")
                return@launch
            }
            lineJob {
                device.folderAbility.pushFile(toRemoteFile.path, *fromLocalFiles)
            }.also {
                emitTipsFlow(it)
                toRemoteFile.refresh()
            }
        }
    }

    fun chmod(device: Device, file: RemoteFile, permission: String) {
        viewModelScope.launch {
            lineJob {
                device.folderAbility.chmod(file.path, permission)
            }.also {
                emitTipsFlow(it)
                file.parent?.refresh()
            }
        }
    }

    fun createDir(device: Device, file: RemoteFile, newFile: String) {
        viewModelScope.launch {
            if (!file.isDirectory) {
                emitTipsFlow("[${file.path}]不是目录")
                return@launch
            }
            lineJob {
                device.folderAbility.createDir(file.path, newFile).onSuccess {
                    file.refresh()
                }.onFailure {
                    it.printStackTrace()
                    emitTipsFlow(it.localizedMessage)
                }
            }
        }
    }

    fun createFile(device: Device, file: RemoteFile, newFile: String) {
        viewModelScope.launch {
            if (!file.isDirectory) {
                emitTipsFlow("[${file.path}]不是目录")
                return@launch
            }
            lineJob {
                device.folderAbility.createFile(file.path, newFile).onSuccess {
                    file.refresh()
                }.onFailure {
                    it.printStackTrace()
                    emitTipsFlow(it.localizedMessage)
                }
            }
        }
    }

    fun delete(device: Device, file: RemoteFile) {
        viewModelScope.launch {
            lineJob {
                device.folderAbility.deleteFile(file.path).onSuccess {
                    file.parent?.refresh()
                }.onFailure {
                    it.printStackTrace()
                    emitTipsFlow(it.localizedMessage)
                }
            }
        }
    }

    fun loadChildren(device: Device, file: RemoteFile) {
        viewModelScope.launch {
            if (file.childrenLoadStates.value != RemoteFileLoadState.NotLoad) {
                return@launch
            }
            file.childrenLoadStates.value = RemoteFileLoadState.Loading
            try {
                val children: List<RemoteFile> = if (!file.isDirectory) {
                    emptyList()
                } else {
                    lineJob {
                        device.folderAbility.refreshPath(file).getOrNull().orEmpty()
                    }
                }
                file.children.value = children
                file.childrenLoadStates.value = RemoteFileLoadState.Loaded
            } catch (e: Throwable) {
                file.children.value = emptyList()
                file.childrenLoadStates.value = RemoteFileLoadState.Error(e.localizedMessage)
            }
        }
    }

    fun getFileDetails(device: Device, file: RemoteFile, forceReload: Boolean = false) {
        viewModelScope.launch {
            val verifyInfo = file.verifyInfo?.takeIf { !forceReload } ?: lineJob {
                device.folderAbility.getFileVerifyInfo(file.path).also {
                    file.verifyInfo = it
                }
            }
            buildString {
                appendLine("路径: ${file.path}")
                append("权限: ${file.permissions}")
                append("  ")
                append("类型: ${file.type.name}")
                append("  ")
                append("大小: ${file.size}")
                appendLine()
                appendLine("修改时间: ${file.modificationTime}")
                appendLine("MD5: ${verifyInfo.md5.fold({ it }, { "获取MD5信息失败 ${it.localizedMessage}" })}")
                appendLine("SHA1: ${verifyInfo.sha1.fold({ it }, { "获取SHA1信息失败 ${it.localizedMessage}" })}")
            }.also {
                emitTipsFlow(it)
            }
        }
    }

    fun restartWithRoot(device: Device, onFinished: () -> Unit) {
        viewModelScope.launch {
            lineJob {
                device.folderAbility.remount()
            }.also {
                emitTipsFlow(it)
            }
            onFinished()
        }
    }
}