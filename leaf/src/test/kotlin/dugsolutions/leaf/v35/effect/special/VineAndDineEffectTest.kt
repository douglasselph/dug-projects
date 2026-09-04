package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectCritterDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectCritterDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VineAndDineEffectTest {

    private val effect = VineAndDineEffect()
    private val nested = GameEffectExecutor { }

    @Test
    fun decisionNamesBothCritterToTrashAndDieToRaise() {
        val firstDie = FixedEffectDie(6, 2)
        val secondDie = FixedEffectDie(10, 3)
        val strategy = ChooseBeeAndSecondDieStrategy()
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(firstDie, secondDie),
            effectStrategy = strategy
        )
        actor.critters.add(Critter.WORM)
        actor.critters.add(Critter.BEE)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val groveBeeCount = game.grove.critters.count(Critter.BEE)

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5
            ),
            nested
        )

        assertEquals(2, firstDie.value)
        assertEquals(8, secondDie.value)
        assertEquals(listOf(Critter.WORM), actor.critters.all)
        assertEquals(groveBeeCount, game.grove.critters.count(Critter.BEE))
        assertTrue(strategy.seen.any {
            it.critter == Critter.BEE && it.die.index == 1
        })
    }

    @Test
    fun cannotExecuteWithoutBothOwnedCritterAndHandDie() {
        val actor = EffectTestFixture.player(1, hand = listOf(FixedEffectDie(6, 2)))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5
        )

        assertFalse(effect.canExecute(request))

        actor.critters.add(Critter.WORM)
        assertTrue(effect.canExecute(request))
    }

    private class ChooseBeeAndSecondDieStrategy : EffectStrategy {
        var seen: List<EffectCritterDieChoice> = emptyList()

        override fun chooseDie(
            request: ChooseEffectDieRequest
        ): EffectDieChoice = request.legalChoices.first()

        override fun chooseCritterAndDie(
            request: ChooseEffectCritterDieRequest
        ): EffectCritterDieChoice {
            seen = request.legalChoices
            return request.legalChoices.first {
                it.critter == Critter.BEE && it.die.index == 1
            }
        }
    }
}
