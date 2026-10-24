package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.orchestration.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.Writer
import java.nio.file.Paths

/** Explicitly opt-in: ./gradlew simulationTest -P... -Dleaf.stress.games=3000 ... */
class BaselineLongRunMemoryTest {
    @Test fun `three thousand successive games do not demonstrate unbounded retained heap`() {
        val games = System.getProperty("leaf.stress.games")?.toIntOrNull() ?: 0
        assumeTrue(games >= 3000, "Long-run check is opt-in; set -Dleaf.stress.games=3000")
        val config = BaselineResyncCardBalanceOptions(
            start = 800000, games = games, players = 4,
            baseSeed = 120000L, strategySeed = 220000L, groveSeed = 320000L,
            plantFile = Paths.get("data/research/resync/resync-current.csv"),
            roundFile = Paths.get("data/research/resync/round-resync-current.csv"),
            output = java.io.File("build/unused-memory-output.tsv")
        )
        val retained = mutableListOf<Long>() // bounded: one Long per 500 games, no Game references
        val monitor = ExperimentMemory(ExperimentMemoryConfig(500, 500)) { sample ->
            retained.add(requireNotNull(sample.retainedHeapBytes))
            println("retained-heap sample=${sample.completedGames} bytes=${sample.retainedHeapBytes}")
        }
        PrintWriter(Writer.nullWriter()).use { writer ->
            BaselineResyncCardBalanceSession(config).use { session ->
                for (n in 1..games) {
                    session.runSample(writer, config.start + n - 1)
                    monitor.afterSample(n, config.start + n - 1)
                    if (n % 500 == 0) writer.flush()
                }
            }
        }
        assertTrue(retained.size >= 6)
        // Compare late measurements with the first post-warmup sample; deliberately
        // generous because GC is nondeterministic, but detect gross retained growth.
        val baseline = retained[1]
        val maxGrowth = maxOf(64L * 1024 * 1024, baseline)
        assertTrue(retained.last() <= baseline + maxGrowth,
            "Post-GC live heap keeps growing: $retained; verify with a profiler")
    }
}
