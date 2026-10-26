package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.RoundDeck
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** A06: named calibration interventions must never be confused with Root Well. */
class A06ResearchControlIdentityTest {
    private val cards by lazy {
        PlantCardRegistry().apply {
            loadFromCsv(
                CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
            )
        }
    }

    /** Construct an A02-compliant 9-slot Grove rather than the older cost-5-only fixture. */
    private fun gameWithTargetedOverrides(overrides: Map<String, GameEffect>): Game {
        val randomizer = Randomizer.create(123L)
        val legalMarket = FirstGameDefault.PLANT_NAMES.map { name ->
            requireNotNull(cards.getCard(name))
        }
        val players = listOf(
            Player(PlayerId(1), DecisionDirector.baseline()),
            Player(PlayerId(2), DecisionDirector.baseline())
        )
        val rounds = RoundCardRegistry(GameEffectConverter()).apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
        }
        val roundDeck = RoundDeck(
            RoundCardManager().apply { loadCards(rounds.getAllCards()) }, randomizer
        ).apply { setup(numBattle = 0, numCultivation = 1) }
        return Game(
            config = GameConfig(
                selectedPlantCards = legalMarket,
                playerDecisionFactories = List(2) { PlayerDecisionFactory.humanBaseline() },
                roundSetup = GameRoundSetup.Ordered(1, 0),
                researchPlantEffectsBySeat = mapOf(0 to overrides),
                seed = 123L
            ),
            grove = Grove(legalMarket, WispDeck(WispCardManager(), randomizer)),
            players = players,
            chronicle = GameChronicle(),
            roundDeck = roundDeck,
            randomizer = randomizer
        )
    }

    @Test
    fun actualCsvCardsHaveDistinctCorrectCanonicalEffects() {
        val fourMore = requireNotNull(cards.getCard("Root_05_02"))
        val well = requireNotNull(cards.getCard("Root_05_04"))
        val queen = requireNotNull(cards.getCard("Flower_17_04"))
        assertEquals("Root Four More", fourMore.title)
        assertEquals(GameEffect.RAISE_DIE_PLUS_4, fourMore.effect)
        assertEquals("Root Well", well.title)
        assertEquals(
            GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE,
            well.effect
        )
        assertEquals(GameEffect.DRAW_TWO_DICE, queen.effect)
        assertNotEquals(fourMore.effect, well.effect)
    }

    @Test
    fun namedRaiseAndDrawOverridesAreOnlyForTargetedSeatAndNotRootWell() {
        val fourMore = requireNotNull(cards.getCard("Root_05_02"))
        val well = requireNotNull(cards.getCard("Root_05_04"))
        val queen = requireNotNull(cards.getCard("Flower_17_04"))
        val game = gameWithTargetedOverrides(mapOf(
            fourMore.name to GameEffect.RESEARCH_RAISE_DIE_PLUS_20,
            queen.name to GameEffect.RESEARCH_DRAW_FOUR_DICE
        ))
        val actor = game.players[0]
        val opponent = game.players[1]
        assertEquals(GameEffect.RESEARCH_RAISE_DIE_PLUS_20, game.effectFor(actor, fourMore))
        assertEquals(GameEffect.RESEARCH_DRAW_FOUR_DICE, game.effectFor(actor, queen))
        assertEquals(well.effect, game.effectFor(actor, well))
        for (card in listOf(fourMore, well, queen)) {
            assertEquals(card.effect, game.effectFor(opponent, card))
        }
    }

    @Test
    fun vpTwentyIsIndependentOfRaiseAndDrawControls() {
        val fourMore = requireNotNull(cards.getCard("Root_05_02"))
        val well = requireNotNull(cards.getCard("Root_05_04"))
        val queen = requireNotNull(cards.getCard("Flower_17_04"))
        for (subject in listOf(fourMore, queen)) {
            val game = gameWithTargetedOverrides(mapOf(subject.name to GameEffect.RESEARCH_GAIN_TWENTY_VP))
            assertEquals(GameEffect.RESEARCH_GAIN_TWENTY_VP, game.effectFor(game.players[0], subject))
            assertEquals(subject.effect, game.effectFor(game.players[1], subject))
            assertEquals(well.effect, game.effectFor(game.players[0], well))
        }
    }
}
