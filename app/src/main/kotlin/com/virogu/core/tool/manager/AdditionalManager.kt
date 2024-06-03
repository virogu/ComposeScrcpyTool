package com.virogu.core.tool.manager

import com.virogu.core.bean.Additional
import com.virogu.core.device.Device
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by Virogu
 * Date 2023/11/13 下午 4:16:41
 **/
interface AdditionalManager {
    val isBusy: StateFlow<Boolean>
    val selectedOnlineDevice: StateFlow<Device?>

    val notification: SharedFlow<String>

    fun exec(additional: Additional)

}
