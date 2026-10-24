package dugsolutions.leaf.simulation.v35.experiment.orchestration

import java.lang.management.ManagementFactory

/** Optional diagnostic measurements. Disabled in normal experiments to avoid GC perturbation. */
data class ExperimentMemoryConfig(
    val sampleEveryGames: Int = 0,
    val retainedHeapEveryGames: Int = 0
) {
    init {
        require(sampleEveryGames >= 0 && retainedHeapEveryGames >= 0)
        require(retainedHeapEveryGames == 0 || sampleEveryGames > 0) {
            "retainedHeapEveryGames requires sampleEveryGames > 0"
        }
        require(retainedHeapEveryGames == 0 || retainedHeapEveryGames % sampleEveryGames == 0) {
            "retainedHeapEveryGames must be a multiple of sampleEveryGames"
        }
    }
}

data class ExperimentMemorySample(
    val completedGames: Int,
    val sampleIndex: Int,
    val heapUsedBytes: Long,
    val heapCommittedBytes: Long,
    val heapMaxBytes: Long,
    val retainedHeapBytes: Long?
)

/** Injectable to make the sampling protocol deterministic in tests. */
interface ExperimentHeapProbe {
    fun usedBytes(): Long
    fun committedBytes(): Long
    fun maxBytes(): Long
    fun collectForDiagnostics()
}

object JvmExperimentHeapProbe : ExperimentHeapProbe {
    private val bean = ManagementFactory.getMemoryMXBean()
    override fun usedBytes(): Long = bean.heapMemoryUsage.used
    override fun committedBytes(): Long = bean.heapMemoryUsage.committed
    override fun maxBytes(): Long = bean.heapMemoryUsage.max
    override fun collectForDiagnostics() = bean.gc()
}

/**
 * Called AFTER a sample's runSample() returns, ensuring its Game and Chronicle are no
 * longer on the caller stack. Never retains games, results or other game-local objects.
 * Diagnostic GC is opt-in and occurs at sparse checkpoints, NEVER after every game.
 */
class ExperimentMemory(
    private val config: ExperimentMemoryConfig,
    private val probe: ExperimentHeapProbe = JvmExperimentHeapProbe,
    private val publish: (ExperimentMemorySample) -> Unit
) {
    fun afterSample(completedGames: Int, sampleIndex: Int) {
        if (config.sampleEveryGames == 0 || completedGames % config.sampleEveryGames != 0) return
        val before = probe.usedBytes()
        val retained = if (config.retainedHeapEveryGames != 0 &&
            completedGames % config.retainedHeapEveryGames == 0) {
            // Two collection requests at sparse checkpoints reduce noise from pending
            // finalizable/reclaimable objects. Not a guarantee of a perfectly quiescent heap.
            probe.collectForDiagnostics()
            probe.collectForDiagnostics()
            probe.usedBytes()
        } else null
        publish(ExperimentMemorySample(completedGames, sampleIndex, before,
            probe.committedBytes(), probe.maxBytes(), retained))
    }
}
