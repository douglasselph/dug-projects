package dugsolutions.leaf.simulation.v35.experiment.orchestration

/** Chunk indices and sample ranges remain stable across full and resumed runs. */
data class ExperimentChunk(
    val number: Int,
    val firstSample: Int,
    val endSampleExclusive: Int
) {
    val games: Int get() = endSampleExclusive - firstSample
}

class ChunkCoordinator(private val config: ExperimentConfig) {
    val totalChunks: Int get() = ((config.games.toLong() + config.chunkSize - 1) / config.chunkSize).toInt()

    fun chunks(): Sequence<ExperimentChunk> = sequence {
        for (index in 0 until totalChunks) {
            val offset = index.toLong() * config.chunkSize
            val endOffset = minOf(config.games.toLong(), offset + config.chunkSize)
            yield(ExperimentChunk(
                number = index + 1,
                firstSample = (config.firstSample.toLong() + offset).toInt(),
                endSampleExclusive = (config.firstSample.toLong() + endOffset).toInt()
            ))
        }
    }
}
