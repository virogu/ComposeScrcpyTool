package com.virogu.core.tool.manager.impl

import com.virogu.core.bean.Additional
import com.virogu.core.tool.connect.DeviceConnect
import com.virogu.core.tool.manager.AdditionalManager
import kotlinx.coroutines.flow.*
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

class AdditionalManagerImpl(deviceConnect: DeviceConnect) : BaseJobManager(), AdditionalManager {

    private val notification by DI.global.instance<MutableSharedFlow<String>>("notification")

    override val isBusy: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val selectedOnlineDevice = deviceConnect.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    override fun exec(additional: Additional) {
        startJob("auxiliary: ${additional.title}") {
            val device = currentDevice ?: return@startJob
            val r = device.additionalAbility.exec(additional)
            if (r.isNotEmpty()) {
                notification
                notification.emit(r)
            }
        }
    }
}