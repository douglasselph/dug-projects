package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OfficialCardSourceTest {
    @Test
    fun `official cards load without research overrides and all Plants have scorers`() {
        val registry = PlantCardRegistry()
        registry.loadFromCsv(
            CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
            CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
        )
        assertEquals(36, registry.getAllCards().size)
        assertEquals(PlantScoringRule.PerGraftedFlower, registry.getCard("Vine_07_04")?.scoringRule)
        val scorers = HumanBaselineCardScorerRegistry()
        for (card in registry.getAllCards()) {
            assertTrue(!card.name.startsWith("zFlower_"))
            assertEquals(card.name, assertNotNull(registry.getCard(card.name)).name)
            assertNotNull(scorers.findByName(card.name), "Missing scorer for ${card.name}: ${card.title}")
        }
        assertEquals(
            registry.getCard("Flower_14_04"),
            registry.getCard("zFlower_14_04")
        )
        assertEquals(GameEffect.GAIN_WORM_AND_BOOST_WORMS_PLUS_1_THIS_ROUND, registry.getCard("Root_07_02")?.effect)
        assertEquals(GameEffect.GAIN_VP_PER_ONE_SHOWING, registry.getCard("Root_07_03")?.effect)
        assertEquals(GameEffect.GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW, registry.getCard("Flower_14_04")?.effect)
        assertEquals(GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW, registry.getCard("Flower_11_03")?.effect)
        assertEquals(GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE, registry.getCard("Flower_17_03")?.effect)
    }

    @Test
    fun `official Wisps and Rounds load`() {
        val converter = GameEffectConverter()
        val wisps = WispCardRegistry(converter)
        wisps.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST))
        assertEquals(12, wisps.getAllCards().size)
        val rounds = RoundCardRegistry(converter)
        rounds.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
        assertEquals(12, rounds.getAllCards().size)
    }
}
