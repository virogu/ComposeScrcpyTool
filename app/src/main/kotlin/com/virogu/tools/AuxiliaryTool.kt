package com.virogu.tools

import com.virogu.bean.DeviceInfo
import com.virogu.bean.ProcessInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by Virogu
 * Date 2023/11/13 下午 4:16:41
 **/
interface AuxiliaryTool {
    val processListFlow: StateFlow<List<ProcessInfo>>
    val isBusy: StateFlow<Boolean>
    val selectedOnlineDevice: StateFlow<DeviceInfo?>

    fun exec(command: Array<String>)

}
