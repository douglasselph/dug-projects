package dugsolutions.leaf.simulation.v35.experiment.orchestration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExperimentOrchestrationTest {
    @Test fun `chunks cover exact global sample interval`() {
        val chunks = ChunkCoordinator(ExperimentConfig(100, 11, 4)).chunks().toList()
        assertEquals(listOf(100 to 104, 104 to 108, 108 to 111), chunks.map { it.firstSample to it.endSampleExclusive })
        assertEquals(listOf(1, 2, 3), chunks.map { it.number })
    }

    @Test fun `runner resumes at chunk boundaries without retaining results`() {
        val events = mutableListOf<ExperimentProgress>()
        val visited = mutableListOf<Int>()
        var clock = 0L
        val timer = ExperimentTiming({ clock }, Clock.fixed(Instant.parse("2026-10-23T20:00:00Z"), ZoneOffset.UTC))
        val runner = ExperimentRunner(ExperimentConfig(10, 7, 3), ExperimentProgressSink { events += it }, timer)
        val summary = runner.run(
            isComplete = { it.number == 1 },
            runSample = { visited += it; clock += 1_000_000_000L }
        )
        assertEquals(listOf(13, 14, 15, 16), visited)
        assertEquals(ExperimentRunner.Summary(2, 1, 4), summary)
        assertEquals(2, timer.samples)
        assertEquals(1, events.filterIsInstance<ExperimentProgress.Skipped>().size)
        assertEquals(2, events.filterIsInstance<ExperimentProgress.Completed>().size)
    }

    @Test fun `commit failure propagates and never records completion`() {
        val events = mutableListOf<ExperimentProgress>()
        val runner = ExperimentRunner(ExperimentConfig(0, 3, 2), ExperimentProgressSink { events += it })
        assertThrows(IllegalStateException::class.java) {
            runner.run(runSample = {}, commitChunk = { error("commit failed") })
        }
        assertEquals(1, events.filterIsInstance<ExperimentProgress.Failed>().size)
        assertTrue(events.none { it is ExperimentProgress.Completed || it is ExperimentProgress.Finished })
    }

    @Test fun `timing excludes skipped slots and formats elapsed durations`() {
        var now = 100L
        val timer = ExperimentTiming({ now }, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        timer.start()
        now += 2_500_000_000L
        timer.finish()
        assertEquals(1, timer.samples)
        assertEquals(5L, timer.estimatedRemaining(2).seconds)
        assertEquals("1h 01m 01s", ExperimentTiming.formatDuration(java.time.Duration.ofSeconds(3661)))
    }

    @Test fun `configuration rejects invalid ranges`() {
        assertThrows(IllegalArgumentException::class.java) { ExperimentConfig(-1, 1, 1) }
        assertThrows(IllegalArgumentException::class.java) { ExperimentConfig(0, 0, 1) }
        assertThrows(IllegalArgumentException::class.java) { ExperimentConfig(0, 1, 0) }
        assertThrows(IllegalArgumentException::class.java) { ExperimentConfig(Int.MAX_VALUE, 2, 1) }
    }
}
