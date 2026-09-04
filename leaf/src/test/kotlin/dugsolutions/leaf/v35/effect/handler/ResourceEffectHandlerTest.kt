package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourceEffectHandlerTest {

    private val handler = ResourceEffectHandler()
    private val nestedExecutor = GameEffectExecutor { }

    @Test
    fun gainWater_movesOneWaterFromGroveToPlayer() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.GAIN_WATER_TOKEN
        )

        handler.execute(request, nestedExecutor)

        assertEquals(1, actor.tokens.waterCount)
        assertEquals(8, game.grove.tokens.waterCount)
    }

    @Test
    fun mulchFromHand_usesChosenDieAndCreatesPendingMulch() {
        val first = FixedEffectDie(6, 3)
        val second = FixedEffectDie(10, 7)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(first, second),
            effectStrategy = LastEffectChoiceStrategy()
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.MULCH_DIE_FROM_HAND
        )

        handler.execute(request, nestedExecutor)

        assertEquals(listOf(first), actor.dice.hand)
        assertEquals(0, actor.tokens.mulchCount)
        assertEquals(
            listOf(Token.PENDING_MULCH(DieSides.D10)),
            actor.tokens.pendingMulchTokens
        )
        assertEquals(8, game.grove.tokens.mulchCount)
    }

    @Test
    fun gainOneVp_isAlwaysExecutableAndAddsVp() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.GAIN_ONE_VP
        )

        assertTrue(handler.canExecute(request))

        handler.execute(request, nestedExecutor)

        assertEquals(1, actor.vp)
    }
}
