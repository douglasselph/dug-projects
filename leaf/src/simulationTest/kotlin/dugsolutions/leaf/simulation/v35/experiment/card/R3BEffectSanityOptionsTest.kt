package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentOverride
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectDecisionRequirements
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class R3BEffectSanityOptionsTest {
    private val grove = listOf("Root_05_02", "Root_07_04", "Root_09_03", "Vine_07_01",
        "Vine_09_01", "Vine_11_04", "Flower_11_03", "Flower_14_02", "Flower_17_04")

    private fun args(target: String, mode: String): Array<String> = arrayOf(
        "--phase=train", "--objective=targeted", "--target-id=$target", "--generations=2",
        "--population=4", "--elites=1", "--games=8", "--sigma=0.6", "--mutations=12",
        "--evolution-seed=620000", "--seed=720000", "--strategy-seed=820000", "--holdout-games=20",
        "--target-boost=15.0", "--target-min-purchase-rate=0.15", "--target-purchase-bonus=0.2",
        "--flower-enabler-boost=8.0", "--effect-variant=$mode",
        "--grove=${grove.joinToString(",")}", "--output-dir=output/experiments/r3b-test",
        "--fingerprint=${"c".repeat(64)}"
    )

    @Test fun `calibration modes accepted only for their intended cards`() {
        assertEquals("root-raise20", R3APlantScreenMain.Options.parse(args("Root_05_02", "root-raise20")).effectVariant)
        assertEquals("queen-draw4", R3APlantScreenMain.Options.parse(args("Flower_17_04", "queen-draw4")).effectVariant)
        assertEquals("disabled", R3APlantScreenMain.Options.parse(args("Root_05_02", "disabled")).effectVariant)
        assertThrows(IllegalArgumentException::class.java) {
            R3APlantScreenMain.Options.parse(args("Flower_17_04", "root-raise20"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            R3APlantScreenMain.Options.parse(args("Root_05_02", "queen-draw4"))
        }
    }

    @Test fun `research effects are explicit and resolvable without canonical mutation`() {
        val overrides = PlantExperimentConfig.of(
            "Root_05_02" to PlantExperimentOverride(effect = GameEffect.RESEARCH_RAISE_DIE_PLUS_20),
            "Flower_17_04" to PlantExperimentOverride(effect = GameEffect.RESEARCH_DRAW_FOUR_DICE)
        )
        assertEquals(GameEffect.RESEARCH_RAISE_DIE_PLUS_20, overrides.overrideFor("Root_05_02")?.effect)
        assertEquals(GameEffect.RESEARCH_DRAW_FOUR_DICE, overrides.overrideFor("Flower_17_04")?.effect)
        assertFalse(PlantExperimentConfig.EMPTY.overrideFor("Root_05_02") != null)
        val dispatcher = DefaultGameEffectExecutor()
        listOf(GameEffect.RESEARCH_NO_EFFECT, GameEffect.RESEARCH_RAISE_DIE_PLUS_20,
            GameEffect.RESEARCH_DRAW_FOUR_DICE).forEach { effect ->
            assertTrue(dispatcher.supports(effect), "Missing executable research effect: $effect")
            assertTrue(GameEffectDecisionRequirements.forEffect(effect) != null, "No decisions contract for $effect")
        }
    }
}
