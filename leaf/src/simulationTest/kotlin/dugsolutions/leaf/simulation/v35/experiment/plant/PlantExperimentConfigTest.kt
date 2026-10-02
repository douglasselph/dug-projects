package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.simulation.v35.experiment.ExperimentConfig
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlantExperimentConfigTest {

    @Test
    fun emptyConfig_hasNoInterventionAndResolvesCanonicalValues() {
        val card = plantCard(
            name = "Vine_07_04",
            cost = 7,
            scoringRule = PlantScoringRule.PerGraftedVine
        )

        val config = PlantExperimentConfig.EMPTY

        assertTrue(config.isEmpty)
        assertNull(config.overrideFor(card.name))
        assertEquals(7, config.costFor(card))
        assertTrue(config.isAvailable(card))
        assertSame(PlantScoringRule.PerGraftedVine, config.scoringRuleFor(card))
        assertEquals(card.effect, config.effectFor(card))
    }

    @Test
    fun override_canBeFoundByStablePlantId() {
        val override = PlantExperimentOverride(cost = 9)
        val config = PlantExperimentConfig.of("Vine_07_01" to override)

        assertEquals(override, config.overrideFor("Vine_07_01"))
        assertEquals(override, config.overrideFor("  VINE_07_01  "))
    }

    @Test
    fun unspecifiedCard_remainsUnspecifiedAndCanonical() {
        val configured = plantCard("Vine_07_01", 7, PlantScoringRule.Fixed(3))
        val untouched = plantCard("Vine_07_02", 7, PlantScoringRule.Fixed(1))
        val config = PlantExperimentConfig.of(
            configured.name to PlantExperimentOverride(cost = 8)
        )

        assertNull(config.overrideFor(untouched))
        assertEquals(untouched.cost, config.costFor(untouched))
        assertTrue(config.isAvailable(untouched))
        assertEquals(untouched.scoringRule, config.scoringRuleFor(untouched))
    }

    @Test
    fun costAvailabilityScoringRuleAndEffect_areIndependentDimensions() {
        val canonical = plantCard("Vine_07_01", 7, PlantScoringRule.Fixed(3))
        val config = PlantExperimentConfig.of(
            canonical.name to PlantExperimentOverride(
                cost = 10,
                available = false,
                scoringRule = PlantScoringRule.Fixed(1),
                effect = GameEffect.RAISE_LOWEST_DIE_PLUS_1
            )
        )

        val override = requireNotNull(config.overrideFor(canonical))
        assertEquals(10, override.cost)
        assertFalse(requireNotNull(override.available))
        assertEquals(PlantScoringRule.Fixed(1), override.scoringRule)
        assertEquals(GameEffect.RAISE_LOWEST_DIE_PLUS_1, override.effect)
        assertEquals(10, config.costFor(canonical))
        assertFalse(config.isAvailable(canonical))
        assertEquals(PlantScoringRule.Fixed(1), config.scoringRuleFor(canonical))
        assertEquals(GameEffect.RAISE_LOWEST_DIE_PLUS_1, config.effectFor(canonical))
    }

    @Test
    fun zeroCost_doesNotImplyExclusion() {
        val canonical = plantCard("Vine_07_01", 7, PlantScoringRule.Fixed(3))
        val config = PlantExperimentConfig.of(
            canonical.name to PlantExperimentOverride(cost = 0)
        )

        assertEquals(0, config.costFor(canonical))
        assertTrue(config.isAvailable(canonical))
        assertNull(config.overrideFor(canonical)?.available)
    }

    @Test
    fun scoringOverride_holdsExistingConditionalRuleWithoutFlatteningToInteger() {
        val canonical = plantCard("Vine_07_04", 7, PlantScoringRule.Fixed(0))
        val config = PlantExperimentConfig.of(
            canonical.name to PlantExperimentOverride(
                scoringRule = PlantScoringRule.PerGraftedVine
            )
        )

        val override = requireNotNull(config.overrideFor(canonical))
        assertSame(PlantScoringRule.PerGraftedVine, override.scoringRule)
        assertSame(PlantScoringRule.PerGraftedVine, config.scoringRuleFor(canonical))
    }

    @Test
    fun experimentConfig_defaultsToCanonicalPlantOverridesAndCanReceiveExplicitConfig() {
        assertSame(PlantExperimentConfig.EMPTY, ExperimentConfig(games = 1).plantOverrides)

        val plantConfig = PlantExperimentConfig.of(
            "Vine_07_01" to PlantExperimentOverride(available = false)
        )
        val experiment = ExperimentConfig(games = 1, plantOverrides = plantConfig)

        assertSame(plantConfig, experiment.plantOverrides)
    }

    private fun plantCard(
        name: String,
        cost: Int,
        scoringRule: PlantScoringRule
    ): PlantCard =
        PlantCard(
            quantity = 1,
            name = name,
            title = name,
            type = PlantType.VINE,
            cost = cost,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = GameEffect.UNKNOWN,
            scoringRule = scoringRule
        )
}
