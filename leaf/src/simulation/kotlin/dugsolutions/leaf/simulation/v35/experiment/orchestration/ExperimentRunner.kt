package dugsolutions.leaf.simulation.v35.experiment.orchestration

import java.time.Duration

/**
 * Executes callbacks sequentially inside one JVM. Neither Game nor per-game results are retained.
 * Timing is performed per non-skipped chunk; completed chunks are measured only after commit succeeds.
 */
class ExperimentRunner(
    private val config: ExperimentConfig,
    private val progress: ExperimentProgressSink = ConsoleExperimentProgress(),
    private val timing: ExperimentTiming = ExperimentTiming()
) {
    data class Summary(val completedChunks: Int, val skippedChunks: Int, val completedGames: Int)

    fun run(
        isComplete: (ExperimentChunk) -> Boolean = { false },
        beginChunk: (ExperimentChunk) -> Unit = {},
        runSample: (Int) -> Unit,
        commitChunk: (ExperimentChunk) -> Unit = {}
    ): Summary {
        var completedChunks = 0
        var skippedChunks = 0
        var completedGames = 0
        var previous: Duration? = null
        var previousWasSkipped = false
        val coordinator = ChunkCoordinator(config)
        for (chunk in coordinator.chunks()) {
            // Like the Bash helper, report the previous measured run at the start of
            // the NEXT slot, but not when the preceding slot was skipped.
            if (!previousWasSkipped && previous != null) {
                progress.publish(ExperimentProgress.PreviousRunTiming(
                    previous,
                    timing.average,
                    timing.estimatedRemaining(coordinator.totalChunks - chunk.number + 1),
                    timing.eta(coordinator.totalChunks - chunk.number + 1)
                ))
            }
            // isComplete is deliberately outside timing, preserving the Bash
            // helper's treatment of skip bookkeeping as unmeasured.
            val skip = isComplete(chunk)
            progress.publish(ExperimentProgress.Started(chunk, coordinator.totalChunks, skip))
            if (skip) {
                skippedChunks++
                previousWasSkipped = true
                previous = null
                progress.publish(ExperimentProgress.Skipped(chunk))
                continue
            }
            previousWasSkipped = false
            timing.start()
            try {
                beginChunk(chunk)
                for (sample in chunk.firstSample until chunk.endSampleExclusive) runSample(sample)
                commitChunk(chunk)
                completedGames += chunk.games
                completedChunks++
                val elapsed = timing.finish()
                previous = elapsed
                progress.publish(ExperimentProgress.Completed(
                    chunk, elapsed, timing.average,
                    timing.estimatedRemaining(coordinator.totalChunks - chunk.number),
                    timing.eta(coordinator.totalChunks - chunk.number)
                ))
            } catch (failure: Throwable) {
                progress.publish(ExperimentProgress.Failed(chunk, failure))
                throw failure
            }
        }
        progress.publish(ExperimentProgress.Finished(
            completedChunks, skippedChunks, timing.measured,
            finalRunDuration = if (previousWasSkipped) null else previous,
            meanDuration = timing.average
        ))
        return Summary(completedChunks, skippedChunks, completedGames)
    }
}
