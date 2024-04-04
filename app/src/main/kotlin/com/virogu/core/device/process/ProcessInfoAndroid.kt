package com.virogu.core.device.process

/**
 * @author Virogu
 * @since 2024-04-02 ÉÏÎç 11:02
 **/
data class ProcessInfoAndroid(
    override val user: String,
    override val uid: String,
    override val pid: String,
    override val processName: String,
    override val packageName: String,
    val params: Map<String, String> = emptyMap()
) : ProcessInfo {
    //val lastRss: String = params["lastRss"].orEmpty()
    override val abi: String = params["mRequiredAbi"] ?: params["requiredAbi"].orEmpty()

    companion object {
        fun parse(info: String): List<ProcessInfo> = info.runCatching {
            val matches = Regex("""(?s)(APP|PERS)\*\s+(.*?)(\*|PID mappings:)""").findAll(this)
            if (matches.count() <= 0) {
                return emptyList()
            }
            matches.mapNotNull { matchesResult ->
                val process = matchesResult.groupValues[2]
                val first = process.reader().readLines().firstOrNull()?.trim() ?: return@mapNotNull null
                //UID 1000 ProcessRecord{a0f6d00 727:com.microsoft.windows.systemapp/u0a48}
                val baseInfo = Regex("""\S+\s+(\d+)\s+ProcessRecord\{(\S+)\s+(\d+):(\S+)/(\S+)}(.*)""").find(first)
                    ?: return@mapNotNull null
                val uid = baseInfo.groupValues.getOrNull(1) ?: return@mapNotNull null
                val pid = baseInfo.groupValues.getOrNull(3) ?: return@mapNotNull null
                val processName = baseInfo.groupValues.getOrNull(4) ?: return@mapNotNull null
                val packageName = processName.split(":").firstOrNull() ?: return@mapNotNull null
                val user: String = baseInfo.groupValues.getOrNull(5).orEmpty().let {
                    if (it.first().isDigit()) {
                        it.toIntOrNull()?.toString() ?: "0"
                    } else {
                        val m = Regex("""(.*?)(\d+)(.*?)""").find(it)
                        m?.groupValues?.getOrNull(2) ?: "0"
                    }
                }
                val maps = Regex("""\s*(\w+)=(\{[^{}]*}|\S+)\s*""").findAll(process).associate { kv ->
                    kv.groupValues[1] to kv.groupValues[2]
                }
                ProcessInfoAndroid(
                    user = user,
                    uid = uid,
                    pid = pid,
                    processName = processName,
                    packageName = packageName,
                    params = maps
                )
            }.toList()
        }.getOrNull().orEmpty()
    }
}

/**
UID = 1000
pid = 727
packageName = com.microsoft.windows.systemapp
uid = 1000
gids = {2001, 1065, 3003, 3007, 1007, 1000, 9997, 1079, 1077}
mRequiredAbi = x86_64
instructionSet = null
class = com.microsoft.windows.systemapp.SystemApp
dir = /system_ext/priv-app/SystemApp/SystemApp.apk
publicDir = /system_ext/priv-app/SystemApp/SystemApp.apk
data = /data/user/0/com.microsoft.windows.systemapp
packageList = {com.microsoft.windows.systemapp}
compat = {160dpi always-compat}
thread = android.app.IApplicationThread$Stub$Proxy@85059ec
pid = 727
lastActivityTime = -1d17h20m18s574ms
startUptimeTime = -1d17h20m19s434ms
startElapsedTime = -1d17h20m19s434ms
persistent = true
removed = false
startSeq = 10
mountMode = INSTALLER
lastPssTime = -9m49s541ms
pssProcState = 0
pssStatType = 0
nextPssTime = +50m10s455ms
lastPss = 19MB
lastSwapPss = 0.00
lastCachedPss = 0.00
lastCachedSwapPss = 0.00
lastRss = 109MB
trimMemoryLevel = 0
lastRequestedGc = -9m19s516ms
lastLowMemory = -9m49s545ms
reportLowMemory = false
currentHostingComponentTypes = 0x202
historicalHostingComponentTypes = 0x202
reportedInteraction = true
time = -9m49s545ms
adjSeq = 3844
lruSeq = 39
mCurSchedGroup = 2
setSchedGroup = 2
systemNoUi = true
curProcState = 0
mRepProcState = 0
setProcState = 0
lastStateTime = -1d17h20m19s384ms
curCapability = LCMN
setCapability = LCMN
 */