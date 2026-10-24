package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.orchestration.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/** Exercise production session/reporting and checkpoint pathways, not mocked game rules. */
class BaselinePersistentJvmRegressionTest {
    private fun options(output: File, games: Int = 7) = BaselineResyncCardBalanceOptions(
        start = 37, games = games, players = 4,
        baseSeed = 120000L, strategySeed = 220000L, groveSeed = 320000L,
        plantFile = Paths.get("data/research/resync/resync-current.csv"),
        roundFile = Paths.get("data/research/resync/round-resync-current.csv"), output = output
    )

    private fun rows(directory: File): List<String> = directory.listFiles()!!
        .filter { it.extension == "tsv" && it.name.startsWith("sample-") }
        .sortedBy { it.name }.flatMapIndexed { i, file ->
            file.readLines().let { if (i == 0) it else it.drop(1) }
        }

    @Test fun `one JVM agrees byte for byte with legacy and different chunk sizes`() {
        val dir = Files.createTempDirectory("baseline-matrix-").toFile()
        try {
            val legacy = File(dir, "legacy.tsv")
            val config = options(legacy)
            BaselineResyncCardBalanceExperiment().execute(config)
            val expected = legacy.readLines()
            assertEquals(BaselineResyncCardBalanceSession.HEADER, expected.first())
            for (chunkSize in listOf(1, 2, 3, 7, 20)) {
                val out = File(dir, "chunks-$chunkSize")
                BaselineResyncCardBalanceChunkedExperiment().execute(config, chunkSize, out)
                assertEquals(expected, rows(out), "Different results for chunk=$chunkSize")
                val chunks = ChunkCoordinator(ExperimentConfig(config.start, config.games, chunkSize)).chunks().toList()
                assertTrue(chunks.all { File(out, "sample-%06d-%06d.complete".format(it.firstSample, it.endSampleExclusive-1)).isFile })
            }
        } finally { dir.deleteRecursively() }
    }

    @Test fun `TSV schema exactly matches legacy columns and global samples`() {
        val dir = Files.createTempDirectory("baseline-schema-").toFile()
        try {
            val config = options(File(dir, "unused.tsv"), 3)
            val out = File(dir, "output")
            BaselineResyncCardBalanceChunkedExperiment().execute(config, 2, out)
            val lines = rows(out)
            val columns = BaselineResyncCardBalanceSession.HEADER.split('\t')
            assertEquals(listOf("sample", "players", "card", "grove_exposed", "legal_buy_options",
                "affordable_options", "selected_buy_options", "purchases", "activations", "final_owned",
                "winner_share_of_owners", "owner_vp_total", "all_player_vp_total"), columns)
            val samples = lines.drop(1).map { line ->
                val fields = line.split('\t')
                assertEquals(columns.size, fields.size)
                assertEquals("4", fields[1])
                fields[0].toInt()
            }
            val sampleCounts = samples.groupingBy { it }.eachCount()
            assertEquals(setOf(37, 38, 39), sampleCounts.keys)
            assertEquals(1, sampleCounts.values.distinct().size)
        } finally { dir.deleteRecursively() }
    }

    @Test fun `validated resume does not modify chunk bytes or resimulate`() {
        val dir = Files.createTempDirectory("baseline-resume-").toFile()
        try {
            val config = options(File(dir, "unused.tsv"), 5)
            val out = File(dir, "chunks")
            val experiment = BaselineResyncCardBalanceChunkedExperiment()
            experiment.execute(config, 2, out, "test-source")
            val before = out.listFiles()!!.filter { it.extension == "tsv" && it.name.startsWith("sample-") }
                .associate { it.name to BaselineChunkCheckpoint.sha256(it) }
            experiment.execute(config, 2, out, "test-source")
            val after = out.listFiles()!!.filter { it.extension == "tsv" && it.name.startsWith("sample-") }
                .associate { it.name to BaselineChunkCheckpoint.sha256(it) }
            assertEquals(before, after)
            assertTrue(out.listFiles()!!.filter { it.extension == "log" }.all { it.readText().contains("SKIP") })
            File(out, "sample-000039-000040.complete").delete()
            experiment.execute(config, 2, out, "test-source")
            assertEquals(before, out.listFiles()!!.filter { it.extension == "tsv" && it.name.startsWith("sample-") }
                .associate { it.name to BaselineChunkCheckpoint.sha256(it) })
            assertTrue(File(out, "sample-000039-000040.complete").isFile)
        } finally { dir.deleteRecursively() }
    }
}
