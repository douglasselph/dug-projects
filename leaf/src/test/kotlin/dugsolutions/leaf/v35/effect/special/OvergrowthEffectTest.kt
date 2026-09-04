package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OvergrowthEffectTest {

    private val effect = OvergrowthEffect()
    private val nested = GameEffectExecutor { }

    @Test
    fun skipsMissingSizesAndUsesSecondAvailableLargerSizeNow() {
        val old = FixedEffectDie(4, 4)
        val actor = EffectTestFixture.player(1, hand = listOf(old))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        empty(game, DieSides.D6)
        empty(game, DieSides.D10)
        empty(game, DieSides.D12)
        /* Remaining larger sizes above D4 are D8 then D20. */

        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW
        )

        assertTrue(effect.canExecute(request))
        effect.execute(request, nested)

        assertEquals(1, actor.dice.handSize)
        assertEquals(20, actor.dice.hand.single().sides)
        assertEquals(0, actor.dice.discardSize)
        assertEquals(1, game.grove.graftBed.count(DieSides.D4))
        assertEquals(9, game.grove.graftBed.count(DieSides.D8))
        assertEquals(8, game.grove.graftBed.count(DieSides.D20))
        assertTrue(game.chronicle.entries.filterIsInstance<GameEntry.Marker>().any {
            it.message.startsWith("ROLL player=1")
        })
    }

    @Test
    fun requiresTwoAvailableLargerSizes_andWaitsForBattlePlacementSupport() {
        val old = FixedEffectDie(4, 4)
        val actor = EffectTestFixture.player(1, hand = listOf(old))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        empty(game, DieSides.D6)
        empty(game, DieSides.D8)
        empty(game, DieSides.D10)
        empty(game, DieSides.D12)

        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW
        )

        assertFalse(effect.canExecute(request))
        assertFalse(effect.canExecute(request.copy(phase = GameEffectPhase.BATTLE)))
    }

    private fun empty(
        game: dugsolutions.leaf.v35.game.Game,
        sides: DieSides
    ) {
        while (game.grove.graftBed.take(sides)) {
            // drain stack
        }
    }
}
