package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleDiePlacementAnalyzerTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)
    private val analyzer = BattleDiePlacementAnalyzer()

    @Test
    fun `expected placement preserves half-value and finds strongest row transition`() {
        val context = context(
            row(StrikeRow.TOP, actorTotal = 5, opponentTotal = 7),
            row(StrikeRow.MIDDLE, actorTotal = 10, opponentTotal = 8)
        )

        val analyses = analyzer(
            context = context,
            dieValue = 3.5,
            mode = BattleAnalysisMode.EXPECTED
        )
        val best = analyses.maxBy { it.tacticalValue }

        assertEquals(StrikeRow.TOP, best.realization)
        assertEquals(BattleAnalysisMode.EXPECTED, best.mode)
        assertEquals(BattleTransition.WIN_FLIPPED, best.swing.rowSwings.single().transition)
        assertEquals(503.5, best.tacticalValue)
    }

    @Test
    fun `projection ignores closed withdrawn and full Strike Squares`() {
        val context = context(
            row(StrikeRow.TOP, actorTotal = 1, opponentTotal = 20, closed = true),
            row(StrikeRow.MIDDLE, actorTotal = 1, opponentTotal = 20, withdrawn = true),
            row(StrikeRow.BOTTOM, actorTotal = 1, opponentTotal = 20, dieCount = 3)
        )

        val analyses = analyzer(
            context = context,
            dieValue = 10.5,
            mode = BattleAnalysisMode.EXPECTED
        )

        assertTrue(analyses.isEmpty())
    }

    private fun context(vararg rows: BattleRowView): DecisionContext =
        DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = actor)
            ),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                rows = rows.toList()
            )
        )

    private fun row(
        row: StrikeRow,
        actorTotal: Int,
        opponentTotal: Int,
        closed: Boolean = false,
        withdrawn: Boolean = false,
        dieCount: Int = 0
    ): BattleRowView =
        BattleRowView(
            row = row,
            closed = closed,
            players = listOf(
                BattlePlayerRowView(
                    playerId = actor,
                    row = row,
                    dice = List(dieCount) { index -> BattleDieView(index, 6, 1) },
                    critters = emptyList(),
                    dieTotal = actorTotal,
                    critterTotal = 0,
                    total = actorTotal,
                    withdrawn = withdrawn
                ),
                BattlePlayerRowView(
                    playerId = opponent,
                    row = row,
                    dice = emptyList(),
                    critters = emptyList(),
                    dieTotal = opponentTotal,
                    critterTotal = 0,
                    total = opponentTotal,
                    withdrawn = false
                )
            )
        )
}
