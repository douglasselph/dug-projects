package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.game.GameStatus
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.RoundValueResolver
import dugsolutions.leaf.v35.round.domain.RoundCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardEffectReadinessTest {

    @Test
    fun revisedOfficialWispsAreExecutableAndNoLongerOnThePendingList() {
        val game = GameEngineTestFixture.game(
            cultivationRounds = 1,
            battleRounds = 0,
            populateWispDeck = true
        )
        assertTrue(game.grove.wispDeck.cards.cards.any { it.name == "Wisp_Award_VP" })
        assertTrue(game.grove.wispDeck.cards.cards.any { it.name == "Wisp_Award_VP2" })
        assertFalse(GameEffect.CHOOSE_PLAYER_WITH_WISPS_TRASH_ONE in CardEffectReadiness.pending)
        assertFalse(GameEffect.EACH_PLAYER_WITH_WISPS_TRASH_ONE in CardEffectReadiness.pending)
        CardEffectReadiness.requireExecutableGame(game)
    }

    @Test
    fun unimplementedResearchRoundOverrideStillBlocksAnActualGame() {
        // Keep the exact deck canonical: setupExact rejects modified catalog
        // definitions. Apply the synthetic unfinished effect through the real
        // research override hook instead.
        val roundValues = object : RoundValueResolver {
            override fun effectFor(card: RoundCard, slot: RoundEffectSlot): GameEffect =
                if (card.name == "Battle_Scatter_Swell" && slot == RoundEffectSlot.SECOND) {
                    GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S
                } else {
                    RoundValueResolver.CANONICAL.effectFor(card, slot)
                }
        }
        val game = GameEngineTestFixture.game(
            cultivationRounds = 0,
            battleRounds = 1,
            roundValues = roundValues
        )
        val registry = RoundCardRegistry(GameEffectConverter()).apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
        }
        val swell = requireNotNull(registry.getCard("Battle_Scatter_Swell"))
        assertEquals(GameEffect.GAIN_UP_TO_D8_AND_USE_NOW, swell.secondEffect.effect)
        game.roundDeck.setupExact(listOf(swell))
        assertEquals(
            GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S,
            game.config.roundValues.effectFor(swell, RoundEffectSlot.SECOND)
        )

        val unusedExecutor = RoundExecutor { _, _ ->
            error("Round execution must not start with a pending synthetic effect")
        }
        val runner = GameRunner(RoundCoordinator(unusedExecutor, unusedExecutor))
        val error = assertFailsWith<IllegalStateException> {
            runner.run(game)
        }
        assertTrue(error.message.orEmpty().contains("GAIN_D4_USE_NOW_THEN_CYCLE_D4S"))
        assertEquals(GameStatus.READY, game.status)
    }

    @Test
    fun pendingEffectCannotBeTreatedAsLegalOrExecutedAsNoOp() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val executor = DefaultGameEffectExecutor()
        val effect = GameEffect.GAIN_D4_USE_NOW_THEN_CYCLE_D4S
        val request = EffectTestFixture.request(game, actor, effect)
        assertFalse(executor.supports(effect))
        val canExecuteError = assertFailsWith<IllegalArgumentException> {
            executor.canExecute(request)
        }
        assertTrue(canExecuteError.message.orEmpty().contains("not yet implemented"))
        val executeError = assertFailsWith<IllegalArgumentException> {
            executor.execute(request)
        }
        assertTrue(executeError.message.orEmpty().contains("not yet implemented"))
    }

    @Test
    fun legacyCardEffectDoesNotBecomeAnAliasForStrongerCurrentVersion() {
        val converter = GameEffectConverter()
        assertEquals(GameEffect.RAISE_ALL_D8S_PLUS_1, converter("Raise all your D8s +1"))
        assertEquals(GameEffect.RAISE_ALL_D8S_PLUS_2, converter("Raise all your D8s +2"))
        assertFalse(GameEffect.RAISE_ALL_D8S_PLUS_2 in CardEffectReadiness.pending)
        assertTrue(DefaultGameEffectExecutor().supports(GameEffect.RAISE_ALL_D8S_PLUS_2))
    }
}
