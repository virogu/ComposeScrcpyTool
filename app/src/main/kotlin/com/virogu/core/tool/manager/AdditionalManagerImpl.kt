package com.virogu.core.tool.manager

import com.virogu.core.bean.Additional
import com.virogu.core.tool.scan.DeviceScan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AdditionalManagerImpl(deviceScan: DeviceScan) : BaseJobManager(), AdditionalManager {

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val selectedOnlineDevice = deviceScan.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    override fun exec(additional: Additional) {
        startJob("auxiliary: ${additional.title}") {
            val device = currentDevice ?: return@startJob
            device.additionalAbility.exec(additional)
        }
    }
}