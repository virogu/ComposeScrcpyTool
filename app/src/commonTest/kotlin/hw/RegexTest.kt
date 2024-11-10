package hw

import org.junit.Test

/**
 * @author Virogu
 * @since 2024-04-07 下午4:07
 **/

internal class RegexTest {
    @Test
    fun ohosPidRegexTest() {
        val s = """
            root             0     1 init
            root             0     2 kthreadd
            root             0     3 rcu_gp
            root             0     4 rcu_par_gp
            root             0     8 mm_percpu_wq
            root             0     9 rcu_tasks_rude_
            root             0    10 rcu_tasks_trace
            root             0    11 ksoftirqd/0
            root             0    12 rcu_sched
        """.trimIndent()
        val pidRegex = Regex("""\s*(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s*""")
        s.trim().split("\n").mapNotNull {
            val r = pidRegex.find(it) ?: return@mapNotNull null
            r.groupValues
            return@mapNotNull "USER:${r.groupValues[1]},UID:${r.groupValues[2]},PID:${r.groupValues[3]},CMD:${r.groupValues[4]}"
        }.also {
            println(it)
        }
    }

    @Test
    fun ohosBundleRegexTest() {
        val s = """
            bundle name [com.example.kikakeyboard]
      bundle name [com.ohos.launcher]
      bundle name [com.ohos.medialibrary.medialibrarydata]
        bundle name []
        """.trimIndent()
        val bundleRegex = Regex(""".*\[(\S+)].*""")
        s.trim().split("\n").mapNotNull {
            val r = bundleRegex.find(it) ?: return@mapNotNull null
            return@mapNotNull r.groupValues[1]
        }.also {
            println(it)
        }
    }

    @Test
    fun adbPidRegexTest() {
        val s = """
            PID #4466: ProcessRecord{35f4c1b 4466:com.android.keychain/1000}
            PID #4494: ProcessRecord{9b3c996 4494:com.android.tv.settings/1000}
        """.trimMargin()
        val regex = Regex("""PID #(\d+):\s+ProcessRecord\{\S+\s+\d+:(\S+)/(\S+)}""")
        s.trim().split("\n").mapNotNull {
            val r = regex.find(it.trim()) ?: return@mapNotNull null
            return@mapNotNull "pid: ${r.groupValues[1]}, uid: ${r.groupValues[3]}, package: ${r.groupValues[2]}"
        }.also {
            println(it)
        }
    }


    @Test
    fun snapshotRegexTest() {
        val r = """
        process: set filename to /data/local/tmp/snapshot_2024-04-24_16-26-59.jpeg
        process: display 0: width 800, height 1280
        snapshot: pixel format is: 3
        snapshot: convert rgba8888 to rgb888 successfully.

        success: snapshot display 0 , write to /data/local/tmp/snapshot_2024-04-24_16-26-59.jpeg as jpeg, width 800, height 1280
    """.trimIndent()
        val regex = Regex("""/\S+\.jpeg""")
        val matchResult = regex.find(r)
        println("value: ${matchResult?.value}")
    }
}