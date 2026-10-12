package dugsolutions.leaf.simulation.v35.replay

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Common console progress format compatible with leaf_experiments.timing Python reports. */
class ResearchProgressReporter(
    private val label: String,
    private val total: Int,
    private val comparisonsPerSeed: Int,
    private val reportEvery: Int = 25
) {
    init { require(total > 0 && reportEvery > 0) }
    private val started = Instant.now()
    private var lastStart = started
    private val clock = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm a").withZone(ZoneId.systemDefault())
    private fun duration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        return "%02d:%02d:%02d".format(s/3600, (s/60)%60, s%60)
    }
    fun start(index: Int, detail: String) {
        lastStart = Instant.now()
        if (index != 1 && index % reportEvery != 1) return
        val elapsed = Duration.between(started, lastStart).seconds
        val remaining = if (index <= 1) null else elapsed * (total-index+1) / (index-1)
        println("\n================================================================")
        println("Run $index of $total starting: $label ($detail)")
        println("Clock: ${clock.format(lastStart)}")
        println("Estimated overall remaining: ${remaining?.let(::duration) ?: "estimating"}")
        println("================================================================")
    }
    fun complete(index: Int) {
        if (index != total && index % reportEvery != 0) return
        val now = Instant.now()
        val elapsed = Duration.between(started, now).seconds
        val last = Duration.between(lastStart, now).seconds
        val remaining = if (index >= total) 0 else elapsed * (total-index) / index
        println("RUN_COMPLETE $index/$total took ${duration(last)}; elapsed=${duration(elapsed)}")
        println("OVERALL_PROGRESS runs=$index/$total comparisons=${index*comparisonsPerSeed}/${total*comparisonsPerSeed} elapsed=${duration(elapsed)} remainingWork=${total-index}; estRemaining=${duration(remaining)}; estimatedExperimentFinish=${if(index==total) "complete" else clock.format(now.plusSeconds(remaining))}")
        System.out.flush()
    }
    fun finish() { println("PROCESS_COMPLETE command=$label elapsed=${duration(Duration.between(started, Instant.now()).seconds)} exit=0") }
}
