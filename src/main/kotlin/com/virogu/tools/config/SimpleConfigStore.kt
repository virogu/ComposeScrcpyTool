package com.virogu.tools.config

import com.virogu.bean.SimpleConfig
import kotlinx.coroutines.flow.StateFlow

interface SimpleConfigStore {

    val simpleConfig: StateFlow<SimpleConfig>

    fun updateSimpleConfig(config: SimpleConfig)

}