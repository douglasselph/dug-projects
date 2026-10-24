package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.orchestration.ChunkCoordinator
import dugsolutions.leaf.simulation.v35.experiment.orchestration.ExperimentConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class BaselineResyncCardBalanceChunkedTest {
    @Test fun `chunk names retain absolute sample numbering including partial tail`() {
        val chunks = ChunkCoordinator(ExperimentConfig(100, 5, 2)).chunks().toList()
        assertEquals(listOf("sample-000100-000101", "sample-000102-000103", "sample-000104-000104"),
            chunks.map(BaselineResyncCardBalanceChunkedExperiment()::stem))
    }

    @Test fun `persistent chunks match existing single batch output byte for byte`() {
        val dir = Files.createTempDirectory("baseline-chunk-equivalence-")
        try {
            val original = dir.resolve("legacy.tsv").toFile()
            val pieces = dir.resolve("chunks").toFile()
            val config = BaselineResyncCardBalanceOptions(
                start = 7, games = 3, players = 4,
                baseSeed = 120000L, strategySeed = 220000L, groveSeed = 320000L,
                plantFile = Paths.get("data/research/resync/resync-current.csv"),
                roundFile = Paths.get("data/research/resync/round-resync-current.csv"),
                output = original
            )
            BaselineResyncCardBalanceExperiment().execute(config)
            BaselineResyncCardBalanceChunkedExperiment().execute(config, 2, pieces)
            val files = pieces.listFiles { f -> f.extension == "tsv" }!!.sortedBy { it.name }
            assertEquals(2, files.size)
            val expected = original.readLines()
            val actual = files.flatMapIndexed { index, file ->
                file.readLines().let { if (index == 0) it else it.drop(1) }
            }
            assertEquals(expected, actual)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
