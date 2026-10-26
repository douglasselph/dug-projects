package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Same target-level complete projection must be independent of a Plant's legacy base. */
class C04CompleteProjectionScaleTest {
    private val actor = PlayerId(1)
    private val rival = PlayerId(2)

    @Test fun `Sapping complete projection is not boosted by legacy intrinsic Plant priority`() {
        val effect = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW
        val scorer = BattleSappingActivationPriority()
        val context = context()
        assertNotNull(scorer.bestTacticalValue(context, effect))
        assertEquals(scorer.score(context, effect, 20)?.total, scorer.score(context, effect, 100)?.total)
    }

    @Test fun `collateral opponent projection is not boosted by legacy intrinsic Plant priority`() {
        val effect = GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
        val scorer = BattleCollateralActivationPriority()
        val context = context()
        assertNotNull(scorer.bestTacticalValue(context, effect))
        assertEquals(scorer.score(context, effect, 20)?.total, scorer.score(context, effect, 100)?.total)
    }

    private fun context(): DecisionContext {
        val row = StrikeRow.TOP
        val own = BattleDieView(0, 20, 3)
        val theirs = BattleDieView(0, 12, 8)
        val rows = listOf(BattleRowView(row, false, listOf(
            BattlePlayerRowView(actor, row, listOf(own), emptyList(), 3, 0, 3, false),
            BattlePlayerRowView(rival, row, listOf(theirs), emptyList(), 8, 0, 8, false)
        )))
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
                id = actor, hand = listOf(DieView(0, 20, 3))
            )),
            battle = BattleView(playerOrder = listOf(actor, rival), rows = rows)
        )
    }
}
