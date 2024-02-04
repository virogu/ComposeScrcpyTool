package com.virogu.core.device.ability

import com.virogu.core.bean.Additional

/**
 * @author Virogu
 * @since 2024-03-28 下午 12:21
 **/
interface DeviceAbilityAdditional {
    suspend fun exec(additional: Additional)
}