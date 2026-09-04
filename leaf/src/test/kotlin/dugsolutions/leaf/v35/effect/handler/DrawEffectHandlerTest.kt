package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawEffectHandlerTest {

    private val handler = DrawEffectHandler()
    private val nested = GameEffectExecutor { }

    @Test
    fun drawTwoDrawsNormallyIntoHand() {
        val actor = EffectTestFixture.player(1)
        actor.dice.addToSupply(FixedEffectDie(4, 3))
        actor.dice.addToSupply(FixedEffectDie(6, 5))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(game, actor, GameEffect.DRAW_TWO_DICE)

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertEquals(2, actor.dice.handSize)
        assertEquals(0, actor.dice.supplySize)
    }

    @Test
    fun rerollUntilThreePlusIgnoresRollRewards() {
        val die = SequenceRollDie(
            sides = 8,
            initial = 6,
            rolls = listOf(1, 2, 3)
        )
        val actor = EffectTestFixture.player(1, hand = listOf(die))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS
            ),
            nested
        )

        assertEquals(3, die.value)
        assertTrue(actor.critters.isEmpty)
        assertTrue(actor.wisps.isEmpty)
    }

    @Test
    fun simpleRerollResolvesNormalRollReward() {
        val die = SequenceRollDie(6, initial = 5, rolls = listOf(1))
        val actor = EffectTestFixture.player(1, hand = listOf(die))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
            ),
            nested
        )

        assertEquals(1, die.value)
        assertEquals(listOf(Critter.BEE), actor.critters.all)
    }

    @Test
    fun discardOneDrawOneMovesSelectedDieToDiscardThenDrawsReplacement() {
        val old = FixedEffectDie(8, 7)
        val replacement = FixedEffectDie(4, 3)
        val actor = EffectTestFixture.player(1, hand = listOf(old))
        actor.dice.addToSupply(replacement)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE
            ),
            nested
        )

        assertEquals(listOf(replacement), actor.dice.hand)
        assertEquals(listOf(old), actor.dice.discard)
    }

    @Test
    fun discardOneDrawTwoDrawsAsManyAsAvailable() {
        val old = FixedEffectDie(8, 7)
        val one = FixedEffectDie(4, 3)
        val two = FixedEffectDie(6, 5)
        val actor = EffectTestFixture.player(1, hand = listOf(old))
        actor.dice.addAllToSupply(listOf(one, two))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE
            ),
            nested
        )

        assertEquals(2, actor.dice.handSize)
        assertTrue(old in actor.dice.discard)
    }

    @Test
    fun raiseThenDrawPerMaxDieCountsAfterTheRaise() {
        val raisedToMax = FixedEffectDie(6, 5)
        val alreadyMax = FixedEffectDie(4, 4)
        val actor = EffectTestFixture.player(
            1,
            hand = listOf(raisedToMax, alreadyMax),
            effectStrategy = LastEffectChoiceStrategy()
        )
        actor.dice.addAllToSupply(
            listOf(
                FixedEffectDie(8, 5),
                FixedEffectDie(10, 6)
            )
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE
            ),
            nested
        )

        assertEquals(6, raisedToMax.value)
        assertEquals(4, actor.dice.handSize)
    }

    @Test
    fun rollDieFromDiscardMovesItToHandThenRollsWithReward() {
        val die = SequenceRollDie(6, initial = 5, rolls = listOf(1))
        val actor = EffectTestFixture.player(1)
        actor.dice.addToDiscard(die)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND
            ),
            nested
        )

        assertTrue(actor.dice.discard.isEmpty())
        assertEquals(listOf(die), actor.dice.hand)
        assertEquals(1, die.value)
        assertEquals(listOf(Critter.BEE), actor.critters.all)
    }

    @Test
    fun effectsThatNeedBattlePlacementAreNotOfferedInBattleYet() {
        val actor = EffectTestFixture.player(1)
        actor.dice.addToSupply(FixedEffectDie(6, 4))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val cultivation = EffectTestFixture.request(game, actor, GameEffect.DRAW_TWO_DICE)

        assertTrue(handler.canExecute(cultivation))
        assertFalse(handler.canExecute(cultivation.copy(phase = GameEffectPhase.BATTLE)))
    }

    private class SequenceRollDie(
        sides: Int,
        initial: Int,
        rolls: List<Int>
    ) : Die(sides) {
        private val rolls = ArrayDeque(rolls)

        init {
            adjustTo(initial)
        }

        override fun roll(): Die {
            val next = if (rolls.isEmpty()) value else rolls.removeFirst()
            adjustTo(next)
            return this
        }
    }
}
