package com.virogu.tools.init

import kotlinx.coroutines.flow.MutableStateFlow

class DefaultInitTool : InitTool {

    override val initStateFlow: MutableStateFlow<InitState> = MutableStateFlow(InitState(true))

    override fun init() {

    }

}