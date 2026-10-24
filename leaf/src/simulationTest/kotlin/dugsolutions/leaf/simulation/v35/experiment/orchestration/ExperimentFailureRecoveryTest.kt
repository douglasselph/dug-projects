package dugsolutions.leaf.simulation.v35.experiment.orchestration

import dugsolutions.leaf.simulation.v35.experiment.baseline.BaselineChunkCheckpoint
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExperimentFailureRecoveryTest {
    @Test fun `failure never commits its partial chunk and resumed run only executes missing chunks`() {
        val dir = Files.createTempDirectory("orchestrator-interrupt-").toFile()
        val header = "sample\tplayers\tcard"
        val checkpoint = BaselineChunkCheckpoint(dir, "inputs-v1", 1, header)
        val config = ExperimentConfig(50, 7, 3)
        var fail = true
        try {
            fun run(): Pair<List<Int>, ExperimentRunner.Summary?> {
                val executed = mutableListOf<Int>()
                var writer: java.io.PrintWriter? = null
                val summary = try {
                    ExperimentRunner(config, ExperimentProgressSink { }).run(
                        isComplete = checkpoint::isComplete,
                        beginChunk = { chunk ->
                            checkpoint.marker(chunk).delete()
                            writer = checkpoint.temporary(chunk).printWriter()
                            writer!!.println(header)
                        },
                        runSample = { sample ->
                            if (fail && sample == 54) throw IllegalStateException("deliberate crash")
                            executed.add(sample)
                            writer!!.println("$sample\t4\tA")
                        },
                        commitChunk = { chunk ->
                            writer!!.close()
                            writer = null
                            checkpoint.commit(chunk)
                        }
                    )
                } catch (e: IllegalStateException) {
                    if (e.message != "deliberate crash") throw e
                    null
                } finally { writer?.close() }
                return executed to summary
            }
            val (partial, initial) = run()
            assertNull(initial)
            assertEquals(listOf(50, 51, 52, 53), partial)
            val chunks = ChunkCoordinator(config).chunks().toList()
            assertTrue(checkpoint.isComplete(chunks[0]))
            assertFalse(checkpoint.isComplete(chunks[1]))
            assertTrue(checkpoint.temporary(chunks[1]).isFile)
            fail = false
            val (resumed, result) = run()
            assertEquals(listOf(53, 54, 55, 56), resumed)
            assertEquals(1, result!!.skippedChunks)
            assertEquals(4, result.completedGames)
            assertTrue(chunks.all(checkpoint::isComplete))
        } finally { dir.deleteRecursively() }
    }

    @Test fun `skipped timing slots excluded from measured average and failure stops later chunks`() {
        var nano = 0L
        val events = mutableListOf<ExperimentProgress>()
        val timer = ExperimentTiming({ nano }, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        val runner = ExperimentRunner(ExperimentConfig(0, 8, 2), ExperimentProgressSink(events::add), timer)
        assertThrows(IllegalArgumentException::class.java) {
            runner.run(isComplete = { it.number == 2 }, runSample = { sample ->
                if (sample == 6) throw IllegalArgumentException("failed")
                nano += 1_000_000_000L
            })
        }
        assertEquals(2, timer.samples)
        assertEquals(4L, timer.measured.seconds)
        assertEquals(2L, timer.average.seconds)
        assertEquals(1, events.filterIsInstance<ExperimentProgress.Skipped>().size)
        assertEquals(1, events.filterIsInstance<ExperimentProgress.Failed>().size)
        assertFalse(events.any { it is ExperimentProgress.Finished })
    }
}
