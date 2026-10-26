package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tactical projections must compete without inherited 50–100 Plant bases. */
class C04ProjectedPlantScaleParityTest {
    private val actor = PlayerId(0)
    private val rival = PlayerId(1)
    private val context = DecisionContext.EMPTY.copy(
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(id = actor)),
        battle = BattleView(
            playerOrder = listOf(actor, rival),
            rows = listOf(BattleRowView(StrikeRow.TOP, false, listOf(
                BattlePlayerRowView(actor, StrikeRow.TOP,
                    listOf(BattleDieView(0, 6, 2)), emptyList(), 2, 0, 2, false),
                BattlePlayerRowView(rival, StrikeRow.TOP,
                    emptyList(), emptyList(), 4, 0, 4, false)
            )))
        )
    )

    @Test fun `direct Raise candidate ignores obsolete Plant base`() {
        val evaluator = BattleDirectDieActivationPriority()
        val effect = GameEffect.RAISE_DIE_PLUS_4
        val tactical = assertNotNull(evaluator.bestTacticalValue(context, effect))
        val low = assertNotNull(evaluator.score(context, effect, PriorityScore(5)))
        val high = assertNotNull(evaluator.score(context, effect, PriorityScore(95)))
        assertEquals(low.total, high.total)
        assertEquals(BattleProjectedActionScore.fromCompleteProjection(tactical, "same").total, low.total)
    }

    @Test fun `pure reroll candidate ignores obsolete Plant base`() {
        val evaluator = BattleRerollActivationPriority()
        val effect = GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS
        val tactical = assertNotNull(evaluator.bestTacticalValue(context, effect))
        val low = assertNotNull(evaluator.score(context, effect, PriorityScore(5)))
        val high = assertNotNull(evaluator.score(context, effect, PriorityScore(95)))
        assertEquals(low.total, high.total)
        assertEquals(BattleProjectedActionScore.fromCompleteProjection(tactical, "same").total, low.total)
    }
}
