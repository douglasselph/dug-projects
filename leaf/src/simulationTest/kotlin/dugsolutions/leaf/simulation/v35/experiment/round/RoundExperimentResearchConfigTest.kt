package dugsolutions.leaf.simulation.v35.experiment.round

import dugsolutions.leaf.simulation.v35.learning.buy.EvalOptions
import dugsolutions.leaf.simulation.v35.learning.buy.TrainOptions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoundExperimentResearchConfigTest {
    @Test
    fun trainAndEvaluateDefaultToNoRoundOverrideFile() {
        assertNull(TrainOptions.parse(emptyList()).roundOverridesPath)
        assertNull(EvalOptions.parse(emptyList()).roundOverridesPath)
    }

    @Test
    fun trainAndEvaluateParseSameRoundOverrideOption() {
        val path = "data/research/4p/round-overrides/example.csv"
        assertEquals(path, TrainOptions.parse(listOf("--round-overrides", path)).roundOverridesPath.toString())
        assertEquals(path, EvalOptions.parse(listOf("--round-overrides", path)).roundOverridesPath.toString())
    }
}
