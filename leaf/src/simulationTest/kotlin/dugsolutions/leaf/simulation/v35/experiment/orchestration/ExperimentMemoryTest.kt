package dugsolutions.leaf.simulation.v35.experiment.orchestration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExperimentMemoryTest {
    private class Probe : ExperimentHeapProbe {
        var gcCalls = 0
        override fun usedBytes() = 100L - gcCalls * 10
        override fun committedBytes() = 200L
        override fun maxBytes() = 1000L
        override fun collectForDiagnostics() { gcCalls++ }
    }

    @Test fun `disabled diagnostics never ask for GC or publish`() {
        val probe = Probe()
        val samples = mutableListOf<ExperimentMemorySample>()
        val monitor = ExperimentMemory(ExperimentMemoryConfig(), probe, samples::add)
        (1..1000).forEach { monitor.afterSample(it, it - 1) }
        assertEquals(0, probe.gcCalls)
        assertTrue(samples.isEmpty())
    }

    @Test fun `sampling is periodic and retained heap uses sparse repeated GC`() {
        val probe = Probe()
        val samples = mutableListOf<ExperimentMemorySample>()
        val monitor = ExperimentMemory(ExperimentMemoryConfig(10, 50), probe, samples::add)
        (1..100).forEach { monitor.afterSample(it, it + 1000) }
        assertEquals((10..100 step 10).toList(), samples.map { it.completedGames })
        assertEquals(4, probe.gcCalls)
        assertEquals(listOf(80L, 60L), samples.mapNotNull { it.retainedHeapBytes })
        assertEquals(1010, samples.first().sampleIndex)
        assertEquals(200L, samples.first().heapCommittedBytes)
    }

    @Test fun `retained collection cannot be enabled without suitable sampling`() {
        assertThrows(IllegalArgumentException::class.java) { ExperimentMemoryConfig(0, 50) }
        assertThrows(IllegalArgumentException::class.java) { ExperimentMemoryConfig(30, 50) }
    }
}
