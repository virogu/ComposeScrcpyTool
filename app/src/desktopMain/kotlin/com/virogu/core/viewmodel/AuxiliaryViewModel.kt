package com.virogu.core.viewmodel

import androidx.lifecycle.viewModelScope
import com.virogu.core.bean.Additional
import com.virogu.core.tool.connect.DeviceConnect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance

/**
 * @author Virogu
 * @since 2024-09-11 上午10:37
 **/

class AuxiliaryViewModel : BaseJobViewModel() {
    private val notification by DI.global.instance<MutableSharedFlow<String>>("notification")
    private val deviceConnect by DI.global.instance<DeviceConnect>()

    val selectedOnlineDevice = deviceConnect.currentSelectedDevice.map {
        it?.takeIf {
            it.isOnline
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val currentDevice get() = selectedOnlineDevice.value

    fun exec(additional: Additional) {
        startJob("auxiliary: ${additional.title}") {
            currentDevice?.also { device ->
                device.additionalAbility.exec(additional).takeIf {
                    it.isNotEmpty()
                }?.also {
                    notification.emit(it)
                }
            }
        }
    }
}