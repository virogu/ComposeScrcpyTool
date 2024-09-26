package com.virogu.core.tool.init

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.File


interface InitTool {
    val workDir: File
    val resourceDir: File
    val initStateFlow: StateFlow<InitState>

    suspend fun waitStart() = initStateFlow.first {
        it.success
    }

    fun init()

}

sealed class InitState(val success: Boolean = false, val msg: String = "", val subMsg: String = "") {

    data object Default : InitState(false, msg = "正在初始化", subMsg = "请稍等...")

    data object Success : InitState(true)

    data class Error(val error: String, val subError: String) : InitState(false, error, subError)
}