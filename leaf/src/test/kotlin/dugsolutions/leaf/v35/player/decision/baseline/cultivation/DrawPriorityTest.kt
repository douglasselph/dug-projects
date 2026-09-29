package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DrawPriorityTest {
    @Test
    fun `preserves existing draw totals while exposing expected value as a component`() {
        val context = contextWithSupply(DieView(0, 4, 3), DieView(1, 20, 8))
        val score = DrawPriority.score(context)

        assertEquals(45, score.total)
        assertEquals(35, score.base)
        assertTrue(score.adjustments.any { it.amount == 10 && it.reason.contains("D4") })
    }

    @Test
    fun `observes visible reference state without turning it into score bonuses`() {
        val context = contextWithSupply(DieView(0, 12, 7), DieView(1, 20, 9))
        val observation = DrawPriority.observe(context)

        assertEquals(12, observation.nextDieSides)
        assertEquals(2.0 / 12.0, observation.rollRewardChance!!, 0.000001)
        assertEquals(2, observation.ownedDiceCount)
        assertEquals(16.0, observation.averageOwnedDieSides!!, 0.000001)
        assertEquals(61, DrawPriority.score(context).total)
    }

    @Test
    fun `uses discard when supply is empty`() {
        val emptyBoard = DecisionContext.EMPTY.self.board
        val context = DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = emptyBoard.copy(discard = listOf(DieView(0, 8, 4), DieView(1, 20, 10)))
            )
        )
        assertEquals(8, DrawPriority.observe(context).nextDieSides)
        assertEquals(53, DrawPriority.score(context).total)
    }

    private fun contextWithSupply(vararg dice: DieView): DecisionContext {
        val board = DecisionContext.EMPTY.self.board
        return DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(board = board.copy(supply = dice.toList()))
        )
    }
}
