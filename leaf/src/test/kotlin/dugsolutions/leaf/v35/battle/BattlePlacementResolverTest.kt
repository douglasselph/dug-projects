package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleStrategy
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleDiePlacementRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleFirstMainActionRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleTurnActionRequest
import dugsolutions.leaf.v35.player.dice.PlayerDice
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BattlePlacementResolverTest {

    @Test
    fun placeNewHandDie_asksBattleStrategyForExactLegalRow() {
        val die = BattleTestFixture.die(8, 6)
        val strategy = RowStrategy(StrikeRow.BOTTOM)
        val player = player(1, strategy, die)
        val other = BattleTestFixture.player(2)
        val state = BattleState(listOf(player, other))

        val placement = BattlePlacementResolver().placeNewHandDie(
            battleState = state,
            player = player,
            die = die,
            reason = BattleDiePlacementReason.MAIN_DRAW
        )

        assertEquals(StrikeRow.BOTTOM, placement.row)
        assertEquals(StrikeRow.BOTTOM, state.grid.locationOf(die)?.row)
        assertEquals(BattleDiePlacementReason.MAIN_DRAW, strategy.request!!.reason)
        assertEquals(8, strategy.request!!.die.sides)
        assertEquals(6, strategy.request!!.die.value)
    }

    @Test
    fun legalRows_excludesClosedAndFullSquares() {
        val player = BattleTestFixture.player(1)
        val other = BattleTestFixture.player(2)
        val state = BattleState(listOf(player, other))

        state.grid.closeRow(StrikeRow.TOP)
        repeat(3) {
            val die = BattleTestFixture.die(6, it + 1)
            player.dice.addToHand(die)
            state.grid.placeDie(player, StrikeRow.MIDDLE, die)
        }

        assertEquals(
            listOf(StrikeRow.BOTTOM),
            BattlePlacementResolver().legalRows(state, player)
        )
    }

    @Test
    fun illegalStrategyRow_isRejectedBeforePlacement() {
        val die = BattleTestFixture.die(6, 5)
        val strategy = RowStrategy(StrikeRow.TOP)
        val player = player(1, strategy, die)
        val other = BattleTestFixture.player(2)
        val state = BattleState(listOf(player, other))
        state.grid.closeRow(StrikeRow.TOP)

        assertFailsWith<InvalidDecisionException> {
            BattlePlacementResolver().placeNewHandDie(
                battleState = state,
                player = player,
                die = die,
                reason = BattleDiePlacementReason.MULCH
            )
        }

        assertTrue(state.grid.locationOf(die) == null)
    }

    private fun player(
        id: Int,
        strategy: BattleStrategy,
        vararg dice: dugsolutions.leaf.v35.random.die.Die
    ): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline().copy(battle = strategy),
            dice = PlayerDice(hand = dice.toList())
        )

    private class RowStrategy(
        private val row: StrikeRow
    ) : BattleStrategy {
        var request: ChooseBattleDiePlacementRequest? = null

        override fun chooseFirstMainAction(
            request: ChooseBattleFirstMainActionRequest
        ): BattleMainAction = request.legalChoices.first()

        override fun chooseTurnAction(
            request: ChooseBattleTurnActionRequest
        ): BattleTurnAction = request.legalChoices.first()

        override fun chooseDiePlacement(
            request: ChooseBattleDiePlacementRequest
        ): StrikeRow {
            this.request = request
            return row
        }
    }
}
