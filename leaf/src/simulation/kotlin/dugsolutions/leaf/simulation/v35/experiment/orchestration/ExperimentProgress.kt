package dugsolutions.leaf.simulation.v35.experiment.orchestration

import java.time.Duration
import java.time.Instant

sealed class ExperimentProgress {
    /** Emitted before the next non-skipped or skipped run heading, as in the Bash helper. */
    data class PreviousRunTiming(
        val duration: Duration,
        val meanDuration: Duration,
        val estimatedRemaining: Duration,
        val eta: Instant
    ) : ExperimentProgress()
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
    data class Finished(
        val completed: Int,
        val skipped: Int,
        val measuredDuration: Duration,
        val finalRunDuration: Duration? = null,
        val meanDuration: Duration = Duration.ZERO,
        val description: String = "Experiment phase complete"
    ) : ExperimentProgress()
}

fun interface ExperimentProgressSink {
    fun publish(event: ExperimentProgress)
}

/** Matches bin/lib/experiment_run_timing.sh while retaining typed event consumers. */
class ConsoleExperimentProgress(private val write: (String) -> Unit = ::println) : ExperimentProgressSink {
    override fun publish(event: ExperimentProgress) {
        when (event) {
            is ExperimentProgress.PreviousRunTiming -> {
                write("")
                write("Previous run took ${ExperimentTiming.formatDuration(event.duration)}; " +
                    "average ${ExperimentTiming.formatDuration(event.meanDuration)}/run; " +
                    "estimated remaining ${ExperimentTiming.formatDuration(event.estimatedRemaining)}; " +
                    "ETA ${ExperimentTiming.formatEta(event.eta)}")
            }
            is ExperimentProgress.Started -> {
                write("")
                write("================================================================")
                val label = if (event.skipped) "SKIP " else ""
                write("Run ${event.chunk.number} of ${event.totalChunks} starting: ${label}samples ${event.chunk.firstSample}-${event.chunk.endSampleExclusive - 1}")
                write("================================================================")
            }
            is ExperimentProgress.Completed -> Unit // Report as previous at the next run, or final at finish.
            is ExperimentProgress.Skipped -> Unit
            is ExperimentProgress.Failed -> write("Run ${event.chunk.number} failed: ${event.cause.message}")
            is ExperimentProgress.Finished -> {
                event.finalRunDuration?.let { duration ->
                    write("")
                    write("Final run took ${ExperimentTiming.formatDuration(duration)}; " +
                        "average ${ExperimentTiming.formatDuration(event.meanDuration)}/run; " +
                        "measured phase total ${ExperimentTiming.formatDuration(event.measuredDuration)}")
                }
                write("")
                write("================================================================")
                write(event.description)
                write("================================================================")
                write("Completed ${event.completed} chunks; skipped ${event.skipped}; " +
                    "measured ${ExperimentTiming.formatDuration(event.measuredDuration)}")
            }
        }
    }
}
