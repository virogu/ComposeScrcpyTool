package com.virogu.tools.init

import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class InitState(val success: Boolean = false, val msg: String = "", val subMsg: String = "")

interface InitTool {
    val workDir: File
    val resourceDir: File
    val initStateFlow: StateFlow<InitState>

    fun init()
}