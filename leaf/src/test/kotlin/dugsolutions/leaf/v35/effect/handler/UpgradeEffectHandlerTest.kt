package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpgradeEffectHandlerTest {

    private val handler = UpgradeEffectHandler()
    private val nestedExecutor = GameEffectExecutor { }

    @Test
    fun compost_isExecutableOnlyWhenImmediateNextSizeIsAvailable() {
        val die = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(1, hand = listOf(die))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.UPGRADE_DIE_FROM_HAND
        )

        assertTrue(handler.canExecute(request))

        repeat(9) {
            assertTrue(game.grove.graftBed.take(DieSides.D10))
        }

        assertFalse(handler.canExecute(request))
    }

    @Test
    fun compost_choosesOnlyEligibleDieAndMovesReplacementToDiscard() {
        val d6 = FixedEffectDie(6, 5)
        val d8 = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(d6, d8),
            effectStrategy = LastEffectChoiceStrategy()
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        repeat(9) {
            assertTrue(game.grove.graftBed.take(DieSides.D10))
        }

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.UPGRADE_DIE_FROM_HAND
            ),
            nestedExecutor
        )

        assertEquals(listOf(d8), actor.dice.hand)
        assertEquals(1, actor.dice.discard.size)
        assertEquals(8, actor.dice.discard.single().sides)
    }
}
