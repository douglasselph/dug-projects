package dugsolutions.leaf.simulation.v35.experiment.orchestration

import java.time.Duration
import java.time.Instant

sealed class ExperimentProgress {
    data class Started(val chunk: ExperimentChunk, val totalChunks: Int, val skipped: Boolean) : ExperimentProgress()
    data class Completed(
        val chunk: ExperimentChunk,
        val duration: Duration,
        val meanDuration: Duration,
        val estimatedRemaining: Duration,
        val eta: Instant
    ) : ExperimentProgress()
    data class Skipped(val chunk: ExperimentChunk) : ExperimentProgress()
    data class Failed(val chunk: ExperimentChunk, val cause: Throwable) : ExperimentProgress()
    data class Finished(val completed: Int, val skipped: Int, val measuredDuration: Duration) : ExperimentProgress()
}

fun interface ExperimentProgressSink {
    fun publish(event: ExperimentProgress)
}

/** Console adapter; output formatting is deliberately separate from the execution loop. */
class ConsoleExperimentProgress(private val write: (String) -> Unit = ::println) : ExperimentProgressSink {
    override fun publish(event: ExperimentProgress) {
        when (event) {
            is ExperimentProgress.Started -> {
                write("")
                write("================================================================")
                val label = if (event.skipped) "SKIP " else ""
                write("Run ${event.chunk.number} of ${event.totalChunks} starting: ${label}samples ${event.chunk.firstSample}-${event.chunk.endSampleExclusive - 1}")
                write("================================================================")
            }
            is ExperimentProgress.Completed -> {
                write("Previous run took ${ExperimentTiming.formatDuration(event.duration)}; " +
                    "average ${ExperimentTiming.formatDuration(event.meanDuration)}/run; " +
                    "estimated remaining ${ExperimentTiming.formatDuration(event.estimatedRemaining)}; " +
                    "ETA ${ExperimentTiming.formatEta(event.eta)}")
            }
            is ExperimentProgress.Skipped -> Unit
            is ExperimentProgress.Failed -> write("Run ${event.chunk.number} failed: ${event.cause.message}")
            is ExperimentProgress.Finished -> write("Experiment complete: ${event.completed} completed, " +
                "${event.skipped} skipped; measured phase total ${ExperimentTiming.formatDuration(event.measuredDuration)}")
        }
    }
}
