package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.SequenceEffectDie
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GustOfPetalsBattleTest {

    private val handler = DrawEffectHandler()
    private val nested = GameEffectExecutor { }

    @Test
    fun battleRerollsChosenOwnDieThenAllQualifyingOpponentDiceInChosenRow() {
        val initialReroll = SequenceEffectDie(6, initial = 5, next = 4)
        val ownLow = FixedEffectDie(8, 3)
        val ownHigh = FixedEffectDie(10, 7)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(initialReroll, ownLow, ownHigh),
            effectStrategy = MiddleRowStrategy()
        )

        val qualifyingReward = SequenceEffectDie(6, initial = 4, next = 1)
        val notQualifying = SequenceEffectDie(8, initial = 2, next = 8)
        val opponentA = EffectTestFixture.player(
            id = 2,
            hand = listOf(qualifyingReward, notQualifying)
        )
        val qualifyingOther = SequenceEffectDie(10, initial = 8, next = 5)
        val opponentB = EffectTestFixture.player(
            id = 3,
            hand = listOf(qualifyingOther)
        )

        val game = EffectTestFixture.game(actor, opponentA, opponentB)
        val battleState = BattleState(listOf(actor, opponentA, opponentB))
        battleState.grid.placeDie(actor, StrikeRow.TOP, initialReroll)
        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, ownLow)
        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, ownHigh)
        battleState.grid.placeDie(opponentA, StrikeRow.MIDDLE, qualifyingReward)
        battleState.grid.placeDie(opponentA, StrikeRow.MIDDLE, notQualifying)
        battleState.grid.placeDie(opponentB, StrikeRow.MIDDLE, qualifyingOther)

        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
        ).copy(
            phase = GameEffectPhase.BATTLE,
            battleState = battleState
        )

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertEquals(4, initialReroll.value)
        assertEquals(1, qualifyingReward.value)
        assertEquals(2, notQualifying.value)
        assertEquals(5, qualifyingOther.value)

        // Roll Rewards belong to the player whose die was rerolled.
        assertEquals(listOf(Critter.BEE), opponentA.critters.all)
        assertTrue(actor.critters.isEmpty)
        assertTrue(opponentB.critters.isEmpty)

        // Rerolls never move the dice.
        assertEquals(StrikeRow.TOP, battleState.grid.locationOf(initialReroll)?.row)
        assertEquals(StrikeRow.MIDDLE, battleState.grid.locationOf(qualifyingReward)?.row)
        assertEquals(StrikeRow.MIDDLE, battleState.grid.locationOf(qualifyingOther)?.row)
    }

    private class MiddleRowStrategy : FirstEffectChoiceStrategy() {
        override fun chooseStrikeRow(
            request: ChooseEffectStrikeRowRequest
        ): StrikeRow = StrikeRow.MIDDLE
    }
}
