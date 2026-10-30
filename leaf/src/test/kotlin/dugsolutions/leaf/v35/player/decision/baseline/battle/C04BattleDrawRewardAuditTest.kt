package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Review-derived C04 cases: reward-rich vs scarce and D4 vs D10. */
class C04BattleDrawRewardAuditTest {
    private fun context(sides: Int, bees: Int = 0, worms: Int = 0, wisps: Int = 0) =
        DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            grove = DecisionContext.EMPTY.grove.copy(
                bees = bees, worms = worms, wispDeckRemaining = wisps
            ),
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    supply = listOf(DieView(0, sides, 1))
                )
            )
        )

    @Test fun `reward probability respects die sides and resource availability`() {
        val d4 = context(4, bees = 1, wisps = 4)
        val d10 = context(10, bees = 1, wisps = 4)
        assertEquals(1.75, BattleDrawRollRewardUtility.expected(d4, 4), 0.00001)
        assertEquals(0.7, BattleDrawRollRewardUtility.expected(d10, 10), 0.00001)
        assertEquals(0.0, BattleDrawRollRewardUtility.expected(context(4), 4), 0.00001)
        assertEquals(0.5, BattleDrawRollRewardUtility.expected(context(4, worms = 1), 4), 0.00001)
        assertEquals(1.25, BattleDrawRollRewardUtility.expected(context(4, wisps = 4), 4), 0.00001)
    }

    @Test fun `Battle Draw counts non-Strike rewards once and never treats them as guaranteed`() {
        val scarce = BattleDrawUtility.score(context(4))
        val rich = BattleDrawUtility.score(context(4, bees = 2, wisps = 4))
        assertEquals(38, scarce.total)
        assertEquals(39, rich.total)
        assertTrue(rich.total > scarce.total)
        assertTrue(rich.adjustments.any { "roll rewards" in it.reason })
    }
}
