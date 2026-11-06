package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.game.GameStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardEffectReadinessTest {

    @Test
    fun unimplementedOfficialWispCannotEnterFullGameUnnoticed() {
        val game = GameEngineTestFixture.game(populateWispDeck = true)
        val unusedExecutor = RoundExecutor { _, _ ->
            error("Round execution must not start when catalog mechanics are missing")
        }
        val runner = GameRunner(RoundCoordinator(unusedExecutor, unusedExecutor))
        val error = assertFailsWith<IllegalStateException> {
            runner.run(game)
        }
        assertTrue(error.message.orEmpty().contains("Wisp_Award_VP"))
        assertTrue(error.message.orEmpty().contains("CHOOSE_PLAYER_WITH_WISPS_TRASH_ONE"))
        assertEquals(GameStatus.READY, game.status)
    }

    @Test
    fun pendingEffectCannotBeTreatedAsLegalOrExecutedAsNoOp() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val executor = DefaultGameEffectExecutor()
        val effect = GameEffect.GAIN_TWO_SUNLIGHT_TOKENS
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
        assertTrue(GameEffect.RAISE_ALL_D8S_PLUS_2 in CardEffectReadiness.pending)
    }
}
