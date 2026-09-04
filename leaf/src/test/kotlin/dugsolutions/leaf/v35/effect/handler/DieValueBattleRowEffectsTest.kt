package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DieValueBattleRowEffectsTest {

    private val handler = DieValueEffectHandler()
    private val nested = GameEffectExecutor { }

    @Test
    fun vineAndPunishmentBattle_reducesEveryOpposingDieInChosenRowByThreeMinimumOne() {
        val actorDie = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(actorDie),
            effectStrategy = RowStrategy(StrikeRow.MIDDLE)
        )
        val opponentMiddleLow = FixedEffectDie(6, 2)
        val opponentMiddleHigh = FixedEffectDie(10, 8)
        val opponentTop = FixedEffectDie(8, 7)
        val opponent = EffectTestFixture.player(
            id = 2,
            hand = listOf(opponentMiddleLow, opponentMiddleHigh, opponentTop)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))

        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, actorDie)
        battleState.grid.placeDie(opponent, StrikeRow.MIDDLE, opponentMiddleLow)
        battleState.grid.placeDie(opponent, StrikeRow.MIDDLE, opponentMiddleHigh)
        battleState.grid.placeDie(opponent, StrikeRow.TOP, opponentTop)

        val request = battleRequest(
            game = game,
            actor = actor,
            battleState = battleState,
            effect = GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3
        )

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertEquals(4, actorDie.value)
        assertEquals(1, opponentMiddleLow.value)
        assertEquals(5, opponentMiddleHigh.value)
        assertEquals(7, opponentTop.value)
    }

    @Test
    fun vineAndPunishmentBattle_rejectsClosedRowReturnedByStrategy() {
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(FixedEffectDie(8, 4)),
            effectStrategy = RowStrategy(StrikeRow.BOTTOM)
        )
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        battleState.grid.closeRow(StrikeRow.BOTTOM)

        val request = battleRequest(
            game = game,
            actor = actor,
            battleState = battleState,
            effect = GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3
        )

        assertFailsWith<InvalidDecisionException> {
            handler.execute(request, nested)
        }
    }

    @Test
    fun bloomBackflipBattle_raisesChosenDieThenFlipsOnlyHigherFlippableOpposingDiceInItsRow() {
        val chosen = FixedEffectDie(8, 2)
        val actor = EffectTestFixture.player(1, hand = listOf(chosen))
        val higher = FixedEffectDie(6, 5)
        val higherD4 = FixedEffectDie(4, 4)
        val notHigher = FixedEffectDie(10, 3)
        val otherRow = FixedEffectDie(12, 9)
        val opponent = EffectTestFixture.player(
            2,
            hand = listOf(higher, higherD4, notHigher, otherRow)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))

        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, chosen)
        battleState.grid.placeDie(opponent, StrikeRow.MIDDLE, higher)
        battleState.grid.placeDie(opponent, StrikeRow.MIDDLE, higherD4)
        battleState.grid.placeDie(opponent, StrikeRow.MIDDLE, notHigher)
        battleState.grid.placeDie(opponent, StrikeRow.TOP, otherRow)

        handler.execute(
            battleRequest(
                game,
                actor,
                battleState,
                GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
            ),
            nested
        )

        assertEquals(3, chosen.value)
        assertEquals(2, higher.value)
        assertEquals(4, higherD4.value) // D4s cannot be flipped.
        assertEquals(3, notHigher.value)
        assertEquals(9, otherRow.value)
        assertEquals(StrikeRow.MIDDLE, battleState.grid.locationOf(chosen)?.row)
    }

    @Test
    fun sappingSnapdragonBattle_raisesThenUsesActualTotalReductionToRaiseAgain() {
        val chosen = FixedEffectDie(12, 3)
        val actor = EffectTestFixture.player(1, hand = listOf(chosen))
        val two = FixedEffectDie(6, 2)
        val one = FixedEffectDie(8, 1)
        val eight = FixedEffectDie(10, 8)
        val opponentA = EffectTestFixture.player(2, hand = listOf(two, one))
        val opponentB = EffectTestFixture.player(3, hand = listOf(eight))
        val game = EffectTestFixture.game(actor, opponentA, opponentB)
        val battleState = BattleState(listOf(actor, opponentA, opponentB))

        battleState.grid.placeDie(actor, StrikeRow.TOP, chosen)
        battleState.grid.placeDie(opponentA, StrikeRow.TOP, two)
        battleState.grid.placeDie(opponentA, StrikeRow.TOP, one)
        battleState.grid.placeDie(opponentB, StrikeRow.TOP, eight)

        handler.execute(
            battleRequest(
                game,
                actor,
                battleState,
                GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW
            ),
            nested
        )

        assertEquals(1, two.value)   // actually reduced by 1
        assertEquals(1, one.value)   // actually reduced by 0
        assertEquals(6, eight.value) // actually reduced by 2
        assertEquals(8, chosen.value) // 3 + 2, then +3 actual reduction
        assertEquals(StrikeRow.TOP, battleState.grid.locationOf(chosen)?.row)
    }

    private fun battleRequest(
        game: dugsolutions.leaf.v35.game.Game,
        actor: dugsolutions.leaf.v35.player.Player,
        battleState: BattleState,
        effect: GameEffect
    ) = EffectTestFixture.request(
        game = game,
        actor = actor,
        effect = effect
    ).copy(
        phase = GameEffectPhase.BATTLE,
        battleState = battleState
    )

    private class RowStrategy(
        private val row: StrikeRow
    ) : FirstEffectChoiceStrategy() {
        override fun chooseStrikeRow(
            request: ChooseEffectStrikeRowRequest
        ): StrikeRow = row
    }
}
