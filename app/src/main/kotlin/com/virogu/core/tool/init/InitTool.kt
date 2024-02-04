package com.virogu.core.tool.init

import kotlinx.coroutines.flow.StateFlow
import java.io.File


interface InitTool {
    val workDir: File
    val resourceDir: File
    val initStateFlow: StateFlow<State>

    fun init()

    data class State(val success: Boolean = false, val msg: String = "", val subMsg: String = "")
}