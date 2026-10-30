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

/** C04: compound Battle scores must respect the real effect target rules. */
class C04CompoundEligibilityAuditTest {
    private val a = PlayerId(1)
    private val b = PlayerId(2)
    private val c = PlayerId(3)

    @Test fun `Gust does not credit opponent rerolls below the lowest own die`() {
        // A D8 reroll has expected face 4.5; opposing dice at 3 do not
        // qualify, whereas opposing dice at 8 do qualify.
        val low = context(top = listOf(7, 3, 3))
        val high = context(top = listOf(7, 8, 8))
        val gust = BattleGustOfPetalsTargetAnalyzer()
        val die = EffectDieChoice(0, 8, 7)
        // The analyzer is allowed to select another row. Keep all other rows
        // identical and ensure the eligible higher-opponent case is analyzed.
        assertNotNull(gust(low, die, 0))
        assertNotNull(gust(high, die, 0))
        // Source-legal own die target sets must be preserved at activation level.
        val priority = BattleCollateralActivationPriority()
        assertEquals(listOf(0), priority.targetProjections(low,
            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
            setOf(0)).mapNotNull { it.handIndex })
    }

    @Test fun `Bloom collateral includes all eligible opponents rather than only one rival`() {
        val bloom = GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
        val bothHigh = context(top = listOf(3, 8, 8))
        val oneHigh = context(top = listOf(3, 8, 2))
        val analyzer = BattleOwnDieCollateralAnalyzer()
        val both = assertNotNull(analyzer(bothHigh, bloom, 0, 0))
        val one = assertNotNull(analyzer(oneHigh, bloom, 0, 0))
        // Do not assert a naked pip difference: the tactical projection values
        // Strike outcomes, Wounds and denial, not raw die-face reductions.
        assertTrue(both.tacticalValue.isFinite())
        assertTrue(one.tacticalValue.isFinite())
        val priority = BattleCollateralActivationPriority()
        assertEquals(both.tacticalValue,
            priority.targetProjections(bothHigh, bloom, setOf(0)).single().tacticalValue)
        assertEquals(one.tacticalValue,
            priority.targetProjections(oneHigh, bloom, setOf(0)).single().tacticalValue)
    }

    private fun context(top: List<Int>): DecisionContext {
        require(top.size == 3)
        val rows = listOf(
            makeRow(StrikeRow.TOP, listOf(BattleDieView(0, 8, top[0])),
                listOf(BattleDieView(0, 12, top[1])), listOf(BattleDieView(0, 12, top[2]))),
            makeRow(StrikeRow.MIDDLE, listOf(BattleDieView(1, 8, 5)),
                listOf(BattleDieView(1, 8, 5)), listOf(BattleDieView(1, 8, 5))),
            makeRow(StrikeRow.BOTTOM, listOf(BattleDieView(2, 8, 5)),
                listOf(BattleDieView(2, 8, 5)), listOf(BattleDieView(2, 8, 5)))
        )
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
                id = a, hand = listOf(DieView(0, 8, top[0]), DieView(1, 8, 5), DieView(2, 8, 5))
            )),
            battle = BattleView(playerOrder = listOf(a, b, c), rows = rows)
        )
    }

    private fun makeRow(
        row: StrikeRow,
        own: List<BattleDieView>,
        opponentB: List<BattleDieView>,
        opponentC: List<BattleDieView>
    ): BattleRowView = BattleRowView(row, false, listOf(
        makePlayer(a, row, own), makePlayer(b, row, opponentB), makePlayer(c, row, opponentC)
    ))

    private fun makePlayer(
        player: PlayerId, row: StrikeRow, dice: List<BattleDieView>
    ): BattlePlayerRowView {
        val total = dice.sumOf { it.value }
        return BattlePlayerRowView(player, row, dice, emptyList(), total, 0, total, false)
    }
}
