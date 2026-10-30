package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** C04 Gust: action choice and actual first-die target share outcome-weighted projections. */
class C04GustOutcomeEnumerationTest {
    private val a = PlayerId(1)
    private val b = PlayerId(2)
    private val c = PlayerId(3)
    private val gust = GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW

    @Test fun `first-reroll outcome expectation feeds Main and own-die target identically`() {
        val context = context()
        val analyzer = BattleGustOfPetalsTargetAnalyzer()
        val priority = BattleCollateralActivationPriority()
        val top = assertNotNull(analyzer(context, EffectDieChoice(0, 8, 3), 0))
        val middle = assertNotNull(analyzer(context, EffectDieChoice(1, 12, 2), 1))
        assertTrue(top.tacticalValue.isFinite())
        assertTrue(middle.tacticalValue.isFinite())
        val targets = priority.targetProjections(context, gust, setOf(0, 1))
        assertEquals(2, targets.size)
        assertEquals(top.tacticalValue, targets.first { it.handIndex == 0 }.tacticalValue, 1e-8)
        assertEquals(middle.tacticalValue, targets.first { it.handIndex == 1 }.tacticalValue, 1e-8)
        assertEquals(targets.maxOf { it.tacticalValue }, assertNotNull(priority.bestTacticalValue(context, gust)), 1e-8)
        assertEquals(listOf(1), priority.targetProjections(context, gust, setOf(1)).map { it.handIndex })
    }

    private fun context(): DecisionContext {
        fun row(row: StrikeRow, index: Int, ownSides: Int, own: Int, rival: Int, third: Int): BattleRowView {
            fun player(id: PlayerId, sides: Int, value: Int) = BattlePlayerRowView(
                id, row, listOf(BattleDieView(index, sides, value)), emptyList(), value, 0, value, false
            )
            return BattleRowView(row, false, listOf(
                player(a, ownSides, own), player(b, 12, rival), player(c, 10, third)
            ))
        }
        val rows = listOf(
            row(StrikeRow.TOP, 0, 8, 3, 8, 6),
            row(StrikeRow.MIDDLE, 1, 12, 2, 9, 5),
            row(StrikeRow.BOTTOM, 2, 8, 6, 7, 8)
        )
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
                id = a, hand = listOf(DieView(0, 8, 3), DieView(1, 12, 2), DieView(2, 8, 6))
            )),
            battle = BattleView(playerOrder = listOf(a, b, c), rows = rows)
        )
    }
}
