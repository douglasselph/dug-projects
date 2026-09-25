package dugsolutions.leaf.simulation.v35.experiment.baseline

import kotlin.test.Test
import kotlin.test.assertEquals

class BaselineResearchOptionsTest {
    @Test
    fun diagnosticDefaultsToTwelveGamesAndOneTerminalCheckpoint() {
        val options = BaselineResearchOptions.parse(listOf("diagnostic"))
        assertEquals(BaselineResearchMode.DIAGNOSTIC, options.mode)
        assertEquals(12, options.games)
        assertEquals(listOf(12), options.checkpoints)
        assertEquals(9, options.plantNames.size)
    }

    @Test
    fun calibrationAcceptsExplicitCumulativeCheckpointsSeedsAndGrove() {
        val plants = (1..9).joinToString(",") { "Plant_$it" }
        val options = BaselineResearchOptions.parse(
            listOf(
                "calibration",
                "--games=500",
                "--checkpoints=100,250,500",
                "--base-seed=123",
                "--strategy-seed=456",
                "--plants=$plants"
            )
        )
        assertEquals(500, options.games)
        assertEquals(listOf(100, 250, 500), options.checkpoints)
        assertEquals(123L, options.baseSeed)
        assertEquals(456L, options.strategyBaseSeed)
        assertEquals((1..9).map { "Plant_$it" }, options.plantNames)
    }
}
