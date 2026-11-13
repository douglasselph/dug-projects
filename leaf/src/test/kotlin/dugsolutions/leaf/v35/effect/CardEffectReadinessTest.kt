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
    fun newlyImplementedOfficialMechanicsAreExecutableButLegacySunlightRaiseIsNot() {
        val executor = DefaultGameEffectExecutor()
        val official = listOf(
            GameEffect.UPGRADE_DIE_CONDITIONAL_TWO_STEPS_AND_USE_NOW,
            GameEffect.RAISE_TWO_DICE_PLUS_1_PER_ROOT_OR_VINE
        )
        for (effect in official) {
            assertTrue(executor.supports(effect), effect.name)
            assertFalse(effect in CardEffectReadiness.pending)
            assertTrue(GameEffectDecisionRequirements.forEffect(effect) != null)
        }
        assertFalse(executor.supports(GameEffect.RAISE_DIE_PLUS_3))
        assertTrue(CardEffectReadiness.pending.isEmpty())
        val game = GameEngineTestFixture.game(cultivationRounds = 1, battleRounds = 1)
        CardEffectReadiness.requireExecutableGame(game)
    }

    @Test
    fun retiredSunlightRaiseCannotExecuteAsAnOfficialEffect() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val executor = DefaultGameEffectExecutor()
        val legacy = GameEffect.RAISE_DIE_PLUS_3
        val request = EffectTestFixture.request(game, actor, legacy)
        assertFalse(executor.supports(legacy))
        assertFalse(executor.canExecute(request))
        val error = assertFailsWith<IllegalArgumentException> { executor.execute(request) }
        assertTrue(error.message.orEmpty().contains("Obsolete Sunlight Raise +3"))
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
