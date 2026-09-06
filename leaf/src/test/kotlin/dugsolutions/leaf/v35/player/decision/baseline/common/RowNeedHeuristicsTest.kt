package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RowNeedHeuristicsTest {
    private val actor = PlayerId(1)

    @Test
    fun closeLosingRow_needsMoreAttentionThanSecureWinningRow() {
        val closeLoss = RowNeedHeuristics.calculate(
            rowView = row(10, 11, 3),
            actorId = actor
        )
        val secureWin = RowNeedHeuristics.calculate(
            rowView = row(15, 8, 5),
            actorId = actor
        )

        assertFalse(closeLoss.currentlyWinning)
        assertTrue(secureWin.currentlyWinning)
        assertTrue(closeLoss.needScore > secureWin.needScore)
        assertEquals(2, closeLoss.pointsToBecomeWinner)
    }

    @Test
    fun losingByFiveOrMore_reportsWoundRiskAndPointsNeededToEscapeIt() {
        val need = RowNeedHeuristics.calculate(
            rowView = row(4, 10, 2),
            actorId = actor
        )

        assertTrue(need.woundRisk)
        assertEquals(2, need.pointsToAvoidWound)
        assertEquals(7, need.pointsToBecomeWinner)
        assertEquals(-6, need.margin)
    }

    @Test
    fun tiedHighWithLowerOpponent_isCurrentlyWinning() {
        val need = RowNeedHeuristics.calculate(
            rowView = row(10, 10, 7),
            actorId = actor
        )

        assertTrue(need.currentlyWinning)
        assertEquals(0, need.margin)
        assertEquals(0, need.pointsToBecomeWinner)
    }

    @Test
    fun everyoneTied_hasNoCurrentWinnerAndOnePointBreaksTie() {
        val need = RowNeedHeuristics.calculate(
            rowView = row(10, 10),
            actorId = actor
        )

        assertFalse(need.currentlyWinning)
        assertEquals(1, need.pointsToBecomeWinner)
    }

    @Test
    fun withdrawnOrClosedRow_isUnavailable() {
        val withdrawn = RowNeedHeuristics.calculate(
            rowView = row(10, 9, actorWithdrawn = true),
            actorId = actor
        )
        val closed = RowNeedHeuristics.calculate(
            rowView = row(10, 9, closed = true),
            actorId = actor
        )

        assertFalse(withdrawn.available)
        assertEquals(0, withdrawn.needScore)
        assertFalse(closed.available)
        assertEquals(0, closed.needScore)
    }

    private fun row(
        actorTotal: Int,
        vararg opponentTotals: Int,
        actorWithdrawn: Boolean = false,
        closed: Boolean = false
    ): BattleRowView =
        BattleRowView(
            row = StrikeRow.TOP,
            closed = closed,
            players = listOf(
                playerRow(actor, actorTotal, actorWithdrawn)
            ) + opponentTotals.mapIndexed { index, total ->
                playerRow(PlayerId(index + 2), total, false)
            }
        )

    private fun playerRow(
        id: PlayerId,
        total: Int,
        withdrawn: Boolean
    ): BattlePlayerRowView =
        BattlePlayerRowView(
            playerId = id,
            row = StrikeRow.TOP,
            dice = emptyList(),
            critters = emptyList<Critter>(),
            dieTotal = total,
            critterTotal = 0,
            total = total,
            withdrawn = withdrawn
        )
}
