package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** End-to-end authoritative CSV identity, routing, strategy requirements and readiness. */
class NextPlantCatalogIntegrationTest {
    @Test
    fun newSunlightAndFlipEffectsAgreeWithActualPlantCatalog() {
        val plants = PlantCardRegistry().apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST))
        }
        val scorer = HumanBaselineCardScorerRegistry()
        val executor = DefaultGameEffectExecutor()
        val expected = mapOf(
            "Flower_11_04" to Pair("Someday Sunflower", GameEffect.GAIN_TWO_SUNLIGHT_TOKENS),
            "Flower_14_02" to Pair("Bloom Backflip", GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE)
        )
        for ((id, spec) in expected) {
            val card = assertNotNull(plants.getCard(id))
            assertEquals(spec.first, card.title, id)
            assertEquals(spec.second, card.effect, id)
            assertEquals(card.effect, scorer.forPlant(card).effect, id)
            assertTrue(executor.supports(card.effect), id)
            assertFalse(card.effect in CardEffectReadiness.pending, id)
            assertNotNull(GameEffectDecisionRequirements.forEffect(card.effect), id)
        }
        assertEquals(
            setOf(EffectDecisionMechanism.EFFECT_DIE),
            GameEffectDecisionRequirements.forEffect(GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE)
                ?.cultivation
        )
        assertEquals(
            setOf(EffectDecisionMechanism.EFFECT_DIE, EffectDecisionMechanism.EFFECT_BATTLE_DIE),
            GameEffectDecisionRequirements.forEffect(GameEffect.FLIP_OWN_DIE_AND_OPPONENT_DIE_IN_BATTLE)
                ?.battle
        )
        // Other pending effects remain guarded: a partial catalog is not declared ready.
        assertTrue(GameEffect.UPGRADE_DIE_CONDITIONAL_TWO_STEPS_AND_USE_NOW in CardEffectReadiness.pending)
    }
}
