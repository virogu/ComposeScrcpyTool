package com.virogu.core.config

import com.virogu.core.bean.SimpleConfig
import kotlinx.coroutines.flow.StateFlow

interface SimpleConfigStore {

    val simpleConfig: StateFlow<SimpleConfig>

    fun updateSimpleConfig(config: SimpleConfig)

}