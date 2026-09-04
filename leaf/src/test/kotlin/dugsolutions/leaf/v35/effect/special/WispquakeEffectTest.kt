package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.SequenceEffectDie
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WispquakeEffectTest {

    @Test
    fun keepsChosenActorDieAndRerollsEveryOtherHandDie() {
        val kept = SequenceEffectDie(6, 4, 6)
        val actorRerolled = SequenceEffectDie(8, 3, 7)
        val opponentRerolled = SequenceEffectDie(10, 5, 9)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(kept, actorRerolled),
            effectStrategy = FirstEffectChoiceStrategy()
        )
        val opponent = EffectTestFixture.player(
            id = 2,
            hand = listOf(opponentRerolled)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val request = EffectTestFixture.request(
            game = game,
            actor = actor,
            effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
            source = GameEffectSource.Wisp(EffectTestFixture.wispquake())
        )

        WispquakeEffect().execute(
            request = request,
            executor = GameEffectExecutor { }
        )

        assertEquals(4, kept.value)
        assertEquals(7, actorRerolled.value)
        assertEquals(9, opponentRerolled.value)
    }
}
