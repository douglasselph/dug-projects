package dugsolutions.leaf.simulation.v35.experiment.orchestration

/**
 * Runs sample callbacks sequentially in the calling JVM. Never retains a Game or a sample result.
 * Consumers own their own chunk output lifecycle, atomic commits and game-specific reporting.
 * A chunk is marked complete only AFTER its consumer successfully commits.
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
        val coordinator = ChunkCoordinator(config)
        for (chunk in coordinator.chunks()) {
            val skip = isComplete(chunk)
            progress.publish(ExperimentProgress.Started(chunk, coordinator.totalChunks, skip))
            if (skip) {
                skippedChunks++
                progress.publish(ExperimentProgress.Skipped(chunk))
                continue
            }
            timing.start()
            try {
                beginChunk(chunk)
                for (sample in chunk.firstSample until chunk.endSampleExclusive) {
                    runSample(sample)
                }
                commitChunk(chunk)
                completedGames += chunk.games
                completedChunks++
                val elapsed = timing.finish()
                progress.publish(ExperimentProgress.Completed(
                    chunk, elapsed, timing.average,
                    timing.estimatedRemaining(coordinator.totalChunks - chunk.number),
                    timing.eta(coordinator.totalChunks - chunk.number)
                ))
            } catch (failure: Throwable) {
                // Never report a failed chunk as complete. Preserve original failure.
                progress.publish(ExperimentProgress.Failed(chunk, failure))
                throw failure
            }
        }
        progress.publish(ExperimentProgress.Finished(completedChunks, skippedChunks, timing.measured))
        return Summary(completedChunks, skippedChunks, completedGames)
    }
}
