package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DieValueEffectHandlerTest {

    private val handler = DieValueEffectHandler()
    private val nestedExecutor = GameEffectExecutor { }

    @Test
    fun raisePlus3_requiresAtLeastOneHandDie() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.RAISE_DIE_PLUS_3
        )

        assertFalse(handler.canExecute(request))

        actor.dice.addToHand(FixedEffectDie(6, 2))

        assertTrue(handler.canExecute(request))
    }

    @Test
    fun raisePlus3_usesChosenLegalHandDie() {
        val first = FixedEffectDie(6, 2)
        val second = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(first, second),
            effectStrategy = LastEffectChoiceStrategy()
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.RAISE_DIE_PLUS_3
        )

        handler.execute(request, nestedExecutor)

        assertEquals(2, first.value)
        assertEquals(7, second.value)
    }

    @Test
    fun unrelatedEffect_isNotClaimedByFamily() {
        val actor = EffectTestFixture.player(
            1,
            hand = listOf(FixedEffectDie(6, 2))
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        assertFalse(
            handler.canExecute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.GAIN_ONE_VP
                )
            )
        )
    }
}
