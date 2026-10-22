package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BattleSappingSnapdragonProjectorTest {
    private val self = PlayerId(1)
    private val opponent = PlayerId(2)
    private val s1 = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW
    private val s2 = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW
    private val projector = BattleSappingSnapdragonProjector()

    @Test fun `S1 applies capped raise and actual reductions in same row`() {
        val context = context(
            row(StrikeRow.TOP, own(StrikeRow.TOP, die(0, 20, 3)), other(StrikeRow.TOP, die(0, 6, 2))),
            row(StrikeRow.MIDDLE, own(StrikeRow.MIDDLE, die(1, 12, 3)), other(StrikeRow.MIDDLE, die(1, 8, 7)))
        )
        val before = context.battle
        val result = assertNotNull(projector.project(context, s1, 0, 0, "S1"))
        assertEquals(listOf(StrikeRow.TOP), result.swing.rowSwings.map { it.row })
        assertEquals(before, context.battle) // projection never mutates game state
        assertNull(projector.project(context, s1, 0, 1, "invalid"))
    }

    @Test fun `S2 changes both rows with distinct raised and anchor dice`() {
        val context = context(
            row(StrikeRow.TOP, own(StrikeRow.TOP, die(0, 20, 3)), other(StrikeRow.TOP, die(0, 8, 5))),
            row(StrikeRow.MIDDLE, own(StrikeRow.MIDDLE, die(1, 12, 3)), other(StrikeRow.MIDDLE, die(1, 8, 7)))
        )
        val result = assertNotNull(projector.project(context, s2, 0, 1, "S2"))
        assertEquals(setOf(StrikeRow.TOP, StrikeRow.MIDDLE), result.swing.rowSwings.map { it.row }.toSet())
        assertNull(projector.project(context, s2, 0, 0, "invalid"))
    }

    @Test fun `S2 anchor reduction still works when no opposing dice are present`() {
        val context = context(
            row(StrikeRow.TOP, own(StrikeRow.TOP, die(0, 20, 3)), other(StrikeRow.TOP, die(0, 8, 5))),
            row(StrikeRow.MIDDLE, own(StrikeRow.MIDDLE, die(1, 12, 3)), other(StrikeRow.MIDDLE))
        )
        assertNotNull(projector.project(context, s2, 0, 1, "empty"))
    }

    @Test fun `second-stage projection does not reapply initial raise`() {
        val before = context(
            row(StrikeRow.TOP, own(StrikeRow.TOP, die(0, 20, 5)), other(StrikeRow.TOP, die(0, 8, 5))),
            row(StrikeRow.MIDDLE, own(StrikeRow.MIDDLE, die(1, 12, 3)), other(StrikeRow.MIDDLE, die(1, 8, 7)))
        )
        val afterFirst = assertNotNull(projector.project(before, s2, 0, 1, "stage2", initialRaisePending = false))
        val withInitial = assertNotNull(projector.project(before, s2, 0, 1, "stage1", initialRaisePending = true))
        assertTrue(afterFirst.swing.rowSwings.size <= withInitial.swing.rowSwings.size)
        assertEquals(before.battle, before.battle)
    }

    private fun context(vararg rows: BattleRowView): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            id = self,
            hand = rows.flatMap { it.forPlayer(self)?.dice.orEmpty() }
                .sortedBy { it.handIndex }.map { DieView(it.handIndex, it.sides, it.value) }
        )),
        battle = BattleView(playerOrder = listOf(self, opponent), rows = rows.toList())
    )
    private fun row(row: StrikeRow, vararg players: BattlePlayerRowView) =
        BattleRowView(row, false, players.toList())
    private fun own(row: StrikeRow, vararg dice: BattleDieView) = player(self, row, *dice)
    private fun other(row: StrikeRow, vararg dice: BattleDieView) = player(opponent, row, *dice)
    private fun player(id: PlayerId, row: StrikeRow, vararg dice: BattleDieView): BattlePlayerRowView {
        val total = dice.sumOf { it.value }
        return BattlePlayerRowView(id, row, dice.toList(), emptyList(), total, 0, total, false)
    }
    private fun die(index: Int, sides: Int, value: Int) = BattleDieView(index, sides, value)
}
