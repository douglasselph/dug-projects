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
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DieValueBattleRowEffectsTest {

    private val handler = DieValueEffectHandler()
    private val nested = GameEffectExecutor { }

    @Test
    fun rootAndScootBattle_raisesThenDiscardsChosenSquareAndWithdrawsOnlyActor() {
        val raised = FixedEffectDie(8, 4)
        val leaving = FixedEffectDie(10, 7)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(raised, leaving),
            effectStrategy = DieAndRowStrategy(
                dieIndex = 0,
                row = StrikeRow.MIDDLE
            )
        )
        val opponentDie = FixedEffectDie(12, 9)
        val opponent = EffectTestFixture.player(
            id = 2,
            hand = listOf(opponentDie)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))

        battleState.grid.placeDie(actor, StrikeRow.TOP, raised)
        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, leaving)
        battleState.grid.placeDie(opponent, StrikeRow.MIDDLE, opponentDie)

        game.grove.critters.remove(Critter.BEE)
        actor.critters.add(Critter.BEE)
        battleState.grid.placeCritter(actor, StrikeRow.MIDDLE, Critter.BEE)
        val groveBeesBefore = game.grove.critters.count(Critter.BEE)

        val request = battleRequest(
            game = game,
            actor = actor,
            battleState = battleState,
            effect = GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE
        )

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertEquals(5, raised.value)
        assertEquals(7, leaving.value)
        assertNull(battleState.grid.locationOf(leaving))
        assertTrue(actor.dice.hand.any { it === raised })
        assertFalse(actor.dice.hand.any { it === leaving })
        assertTrue(actor.dice.discard.any { it === leaving })
        assertTrue(
            battleState.grid.isPlayerWithdrawn(
                actor.id,
                StrikeRow.MIDDLE
            )
        )
        assertFalse(battleState.grid.isRowClosed(StrikeRow.MIDDLE))
        assertFalse(
            battleState.grid.isPlayerWithdrawn(
                opponent.id,
                StrikeRow.MIDDLE
            )
        )
        assertEquals(
            StrikeRow.MIDDLE,
            battleState.grid.locationOf(opponentDie)?.row
        )
        assertTrue(
            battleState.grid.square(actor.id, StrikeRow.MIDDLE).isEmpty
        )
        assertEquals(
            groveBeesBefore + 1,
            game.grove.critters.count(Critter.BEE)
        )
    }

    @Test
    fun rootAndScootBattle_mayWithdrawFromAnEmptyStrikeSquare() {
        val die = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(die),
            effectStrategy = DieAndRowStrategy(
                dieIndex = 0,
                row = StrikeRow.BOTTOM
            )
        )
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        battleState.grid.placeDie(actor, StrikeRow.TOP, die)

        handler.execute(
            battleRequest(
                game,
                actor,
                battleState,
                GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE
            ),
            nested
        )

        assertEquals(5, die.value)
        assertTrue(
            battleState.grid.isPlayerWithdrawn(
                actor.id,
                StrikeRow.BOTTOM
            )
        )
        assertEquals(
            StrikeRow.TOP,
            battleState.grid.locationOf(die)?.row
        )
    }

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

    private class DieAndRowStrategy(
        private val dieIndex: Int,
        private val row: StrikeRow
    ) : FirstEffectChoiceStrategy() {
        override fun chooseDie(
            request: ChooseEffectDieRequest
        ): EffectDieChoice =
            request.legalChoices.first { it.index == dieIndex }

        override fun chooseStrikeRow(
            request: ChooseEffectStrikeRowRequest
        ): StrikeRow = row
    }

    private class RowStrategy(
        private val row: StrikeRow
    ) : FirstEffectChoiceStrategy() {
        override fun chooseStrikeRow(
            request: ChooseEffectStrikeRowRequest
        ): StrikeRow = row
    }
}
