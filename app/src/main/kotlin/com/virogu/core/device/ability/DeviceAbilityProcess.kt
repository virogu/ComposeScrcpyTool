package com.virogu.core.device.ability

import com.virogu.core.bean.ProcessInfo

/**
 * @author Virogu
 * @since 2024-03-27 下午 8:35
 **/
interface DeviceAbilityProcess {
    suspend fun refresh(): List<ProcessInfo>

    suspend fun killProcess(info: ProcessInfo): Result<String>

    suspend fun forceStopProcess(info: ProcessInfo): Result<String>
}