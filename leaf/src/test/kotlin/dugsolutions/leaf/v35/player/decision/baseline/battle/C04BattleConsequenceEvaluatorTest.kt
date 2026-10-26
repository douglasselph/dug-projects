package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class C04BattleConsequenceEvaluatorTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)
    private val evaluator = BattleConsequenceEvaluator()

    @Test
    fun `placed expected die uses same tactical swing and strike VP projection`() {
        val context = context(row(StrikeRow.TOP, 5, 7))
        val values = evaluator.placedDie(context, 3.5, BattleAnalysisMode.EXPECTED)
        assertEquals(1, values.size)
        assertEquals(503.5, values.single().tacticalPreference)
        assertEquals(2, values.single().projectedStrikeVpDelta)
        assertEquals(listOf(BattleTransition.WIN_FLIPPED), values.single().transitions)
        assertEquals(BattleAnalysisMode.EXPECTED, values.single().mode)
    }

    @Test
    fun `full closed or withdrawn rows cannot receive a projected die`() {
        val context = context(
            row(StrikeRow.TOP, 1, 9, closed = true),
            row(StrikeRow.MIDDLE, 1, 9, withdrawn = true),
            row(StrikeRow.BOTTOM, 1, 9, diceCount = 3)
        )
        assertTrue(evaluator.placedDie(context, 10.5, BattleAnalysisMode.EXPECTED).isEmpty())
    }

    @Test
    fun `direct row realization agrees exactly with existing action analyzer`() {
        val context = context(row(StrikeRow.TOP, 5, 7))
        val before = BattleActionRowState(
            StrikeRow.TOP, true, false, false, false, -2.0, -2.0,
            5.0, mapOf(opponent to 7.0)
        )
        val after = before.copy(
            currentlyWinning = true, scoreMargin = 1.5, liveThreatMargin = 1.5,
            ownTotal = 8.5
        )
        val candidate = BattleActionRealization("raise target", BattleAnalysisMode.DETERMINISTIC,
            listOf(BattleActionRowChange(before, after)))
        val original = BattleActionAnalyzer()(context, candidate)
        val projected = evaluator.evaluate(context, candidate)
        assertEquals(original.tacticalValue, projected.tacticalPreference)
        assertEquals(original.vpImpact.gain, projected.projectedStrikeVpDelta)
    }

    private fun context(vararg rows: BattleRowView): DecisionContext =
        DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = actor)
            ),
            battle = BattleView(playerOrder = listOf(actor, opponent), donePlayerIds = emptySet(), rows = rows.toList())
        )

    private fun row(
        row: StrikeRow, own: Int, other: Int,
        closed: Boolean = false, withdrawn: Boolean = false, diceCount: Int = 0
    ): BattleRowView = BattleRowView(
        row = row, closed = closed,
        players = listOf(
            BattlePlayerRowView(actor, row, List(diceCount) { index -> BattleDieView(index, 6, 1) },
                emptyList(), own, 0, own, withdrawn),
            BattlePlayerRowView(opponent, row, emptyList(), emptyList(), other, 0, other, false)
        )
    )
}
