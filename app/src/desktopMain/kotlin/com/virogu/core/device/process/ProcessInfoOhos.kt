package com.virogu.core.device.process

/**
 * @author Virogu
 * @since 2024-04-02 上午 11:02
 **/
data class ProcessInfoOhos(
    override val user: String,
    override val uid: String,
    override val pid: String,
    override val packageName: String,
) : ProcessInfo {
    override val processName: String = packageName
    override val abi: String = ""
}