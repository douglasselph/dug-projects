package dugsolutions.leaf.simulation.v35.experiment.orchestration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExperimentTimingParityTest {
    @Test fun `previous timing appears before next heading with correct remaining slots`() {
        var nano = 0L
        val events = mutableListOf<ExperimentProgress>()
        val timer = ExperimentTiming({ nano }, Clock.fixed(Instant.parse("2026-10-23T20:00:00Z"), ZoneOffset.UTC))
        val result = ExperimentRunner(ExperimentConfig(0, 5, 2), ExperimentProgressSink { events.add(it) }, timer)
            .run(runSample = { nano += 2_000_000_000L })
        assertEquals(5, result.completedGames)
        val prior = events.filterIsInstance<ExperimentProgress.PreviousRunTiming>()
        assertEquals(2, prior.size)
        assertEquals(4L, prior[0].duration.seconds)
        assertEquals(8L, prior[0].estimatedRemaining.seconds) // two slots remain
        assertEquals(4L, prior[1].meanDuration.seconds)
        assertEquals(4L, prior[1].estimatedRemaining.seconds) // one slot remains
        val last = events.last() as ExperimentProgress.Finished
        assertEquals(2L, last.finalRunDuration!!.seconds)
        assertEquals(10L, last.measuredDuration.seconds)
        assertTrue(events.indexOf(prior[0]) < events.indexOf(events.filterIsInstance<ExperimentProgress.Started>()[1]))
    }

    @Test fun `skips excluded from elapsed average and preceding skip suppresses previous timing`() {
        var nano = 0L
        val events = mutableListOf<ExperimentProgress>()
        val timer = ExperimentTiming({ nano }, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        ExperimentRunner(ExperimentConfig(0, 8, 2), ExperimentProgressSink { events.add(it) }, timer).run(
            isComplete = { it.number == 2 || it.number == 4 },
            runSample = { nano += 1_000_000_000L }
        )
        assertEquals(2, timer.samples)
        assertEquals(4L, timer.measured.seconds)
        assertEquals(2L, timer.average.seconds)
        assertEquals(2, events.filterIsInstance<ExperimentProgress.PreviousRunTiming>().size)
        assertNull((events.last() as ExperimentProgress.Finished).finalRunDuration)
    }

    @Test fun `console output retains bash timing headings and final totals`() {
        val lines = mutableListOf<String>()
        val sink = ConsoleExperimentProgress(lines::add)
        var nano = 0L
        ExperimentRunner(ExperimentConfig(25, 4, 2), sink,
            ExperimentTiming({ nano }, Clock.fixed(Instant.parse("2026-10-23T20:00:00Z"), ZoneOffset.UTC)))
            .run(runSample = { nano += 1_000_000_000L })
        val output = lines.joinToString("\n")
        assertTrue(output.contains("Run 1 of 2 starting: samples 25-26"))
        assertTrue(output.contains("Previous run took 2s; average 2s/run; estimated remaining 2s; ETA"))
        assertTrue(output.contains("Final run took 2s; average 2s/run; measured phase total 4s"))
        assertTrue(output.contains("Experiment phase complete"))
    }
}
