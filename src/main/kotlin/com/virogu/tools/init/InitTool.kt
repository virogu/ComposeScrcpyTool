package com.virogu.tools.init

import kotlinx.coroutines.flow.StateFlow

data class InitState(val success: Boolean = false, val msg: String = "", val subMsg: String = "")

interface InitTool {

    val initStateFlow: StateFlow<InitState>

    fun init()

}