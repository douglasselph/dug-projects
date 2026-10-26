package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class C04DirectDieActivationPriorityTest {
    private val actor = PlayerId(0)
    private val opponent = PlayerId(1)
    private val direct = BattleDirectDieActivationPriority()
    private val rowChange = BattleOwnTotalChangeAnalyzer()

    @Test
    fun `direct raise tactical result agrees with same row consequence projector`() {
        val context = context(4, 3, 9)
        val expected = rowChange(
            context, 0, StrikeRow.TOP, 1.0, BattleAnalysisMode.DETERMINISTIC
        )!!.tacticalValue
        assertEquals(expected, direct.bestTacticalValue(context, GameEffect.RAISE_DIE_PLUS_4))
    }

    @Test
    fun `capped die at maximum never claims a phantom four pip gain`() {
        val context = context(6, 6, 8)
        val expected = rowChange(
            context, 0, StrikeRow.TOP, 0.0, BattleAnalysisMode.DETERMINISTIC
        )!!.tacticalValue
        assertEquals(expected, direct.bestTacticalValue(context, GameEffect.RAISE_DIE_PLUS_4))
    }

    @Test
    fun `unsupported compound effects do not get a fabricated direct projection`() {
        val context = context(6, 2, 8)
        assertNull(direct.bestTacticalValue(context, GameEffect.GAIN_ONE_VP))
        assertNull(direct.bestTacticalValue(context, GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW))
    }

    @Test
    fun `closed row has no legal die-change target`() {
        val context = context(6, 2, 8, closed = true)
        assertNull(direct.bestTacticalValue(context, GameEffect.RAISE_DIE_PLUS_4))
    }

    private fun context(sides: Int, face: Int, opponentTotal: Int, closed: Boolean = false) =
        DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(id = actor)),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                rows = listOf(BattleRowView(StrikeRow.TOP, closed, listOf(
                    BattlePlayerRowView(actor, StrikeRow.TOP, listOf(BattleDieView(0, sides, face)), emptyList(), face, 0, face, false),
                    BattlePlayerRowView(opponent, StrikeRow.TOP, emptyList(), emptyList(), opponentTotal, 0, opponentTotal, false)
                )))
            )
        )
}
