package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Real official CSV -> converter -> executable identity -> Human Baseline scorer. */
class RevisedRaiseCatalogIntegrationTest {
    @Test
    fun currentCsvIdentitiesAndScorersAgreeWithoutReusingOldEffects() {
        val plants = PlantCardRegistry().apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST))
        }
        val scorers = HumanBaselineCardScorerRegistry()
        val executor = DefaultGameEffectExecutor()
        val expected = mapOf(
            "Vine_07_04" to Triple("Vine Yield", 7, GameEffect.RAISE_ONE_DIE_PLUS_1_PER_GRAFTED_FLOWER),
            "Vine_11_02" to Triple("Berry Tasty", 11, GameEffect.RAISE_ALL_D8S_PLUS_2),
            "Vine_11_03" to Triple("Rise and Vine", 11, GameEffect.RAISE_ALL_DICE_PLUS_2),
            "Flower_11_02" to Triple("Bloom Backbone", 11, GameEffect.RAISE_ALL_DICE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER)
        )
        for ((id, spec) in expected) {
            val card = assertNotNull(plants.getCard(id))
            assertEquals(spec.first, card.title, id)
            assertEquals(spec.second, card.cost, id)
            assertEquals(spec.third, card.effect, id)
            assertEquals(card.effect, scorers.forPlant(card).effect, id)
            assertTrue(executor.supports(card.effect), id)
            assertFalse(card.effect in CardEffectReadiness.pending, id)
            assertNotNull(GameEffectDecisionRequirements.forEffect(card.effect), id)
        }
        assertEquals(PlantScoringRule.PerGraftedFlower, plants.getCard("Vine_07_04")?.scoringRule)
        assertEquals(PlantScoringRule.Fixed(3), plants.getCard("Vine_11_02")?.scoringRule)
        assertEquals(PlantScoringRule.Fixed(2), plants.getCard("Vine_11_03")?.scoringRule)
        assertEquals(PlantScoringRule.Fixed(1), plants.getCard("Flower_11_02")?.scoringRule)

        // Saplink Trellis moved to the old Rise and Vine ID, but its stronger
        // two-target mechanic must remain pending until NC-08 is approved and implemented.
        val saplink = assertNotNull(plants.getCard("Vine_09_03"))
        assertEquals("Saplink Trellis", saplink.title)
        assertEquals(GameEffect.RAISE_TWO_DICE_PLUS_1_PER_ROOT_OR_VINE, saplink.effect)
        assertEquals(saplink.effect, scorers.forPlant(saplink).effect)
        assertTrue(saplink.effect in CardEffectReadiness.pending)
        assertFalse(executor.supports(saplink.effect))
    }
}
