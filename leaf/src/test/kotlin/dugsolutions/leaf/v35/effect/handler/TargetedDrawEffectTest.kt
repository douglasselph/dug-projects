package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDiceChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TargetedDrawEffectTest {

    private val handler = DrawEffectHandler()
    private val nested = GameEffectExecutor { }

    @Test
    fun rootRecallCultivation_usesOneDecisionContainingEntireDiscardSubset() {
        val strategy = RecallSubsetStrategy()
        val keep = FixedEffectDie(4, 4)
        val discardOne = FixedEffectDie(6, 5)
        val discardTwo = FixedEffectDie(8, 7)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(keep, discardOne, discardTwo),
            effectStrategy = strategy
        )
        val drawnOne = FixedEffectDie(10, 9)
        val drawnTwo = FixedEffectDie(12, 11)
        actor.dice.addAllToSupply(listOf(drawnOne, drawnTwo))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE
            ),
            nested
        )

        assertEquals(3, strategy.seenChoices.size)
        assertEquals(
            listOf(keep, drawnOne, drawnTwo),
            actor.dice.hand
        )
        assertEquals(
            listOf(discardOne, discardTwo),
            actor.dice.discard
        )
    }

    @Test
    fun rootRecallBattle_usesOneDieTargetAndRerollsWithoutMovingIt() {
        val die = SequenceDie(8, initial = 7, rerolled = 3)
        val actor = EffectTestFixture.player(1, hand = listOf(die))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE
        ).copy(phase = GameEffectPhase.BATTLE)

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertEquals(3, die.value)
        assertEquals(listOf(die), actor.dice.hand)
    }

    private class RecallSubsetStrategy : EffectStrategy {
        var seenChoices: List<EffectDieChoice> = emptyList()

        override fun chooseDie(
            request: ChooseEffectDieRequest
        ): EffectDieChoice = request.legalChoices.first()

        override fun chooseDice(
            request: ChooseEffectDiceRequest
        ): EffectDiceChoice {
            seenChoices = request.legalChoices
            return EffectDiceChoice(request.legalChoices.drop(1))
        }
    }

    private class SequenceDie(
        sides: Int,
        initial: Int,
        private val rerolled: Int
    ) : Die(sides) {
        init { adjustTo(initial) }

        override fun roll(): Die {
            adjustTo(rerolled)
            return this
        }
    }
}
