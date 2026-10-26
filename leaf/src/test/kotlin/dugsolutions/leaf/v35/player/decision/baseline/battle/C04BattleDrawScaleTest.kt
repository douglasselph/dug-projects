package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class C04BattleDrawScaleTest {
    private fun context(sides: Int?) = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            id = PlayerId(0),
            supply = sides?.let { listOf(DieView(0, it, 1)) } ?: emptyList()
        ))
    )

    @Test fun `Draw has Battle-specific contribution and never uses Cultivation times four`() {
        val d4 = BattleDrawUtility.score(context(4))
        val d20 = BattleDrawUtility.score(context(20))
        assertEquals(38, d4.total) // 35 + round(2.5)
        assertEquals(46, d20.total) // 35 + round(10.5)
        assertTrue(d4.adjustments.any { "not Cultivation x4" in it.reason })
    }

    @Test fun `empty supply and discard give zero Draw utility`() {
        assertEquals(0, BattleDrawUtility.score(context(null)).total)
    }
}
