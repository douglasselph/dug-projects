package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Selection-level checks: same Main scorer, no fabricated fixed Round premiums. */
class C04RoundScaleMixedSelectionTest {
    private val context = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            id = PlayerId(0), supply = listOf(DieView(0, 4, 1))
        ))
    )

    private fun round(first: GameEffect) = RoundCard(
        quantity = 1,
        name = "C04_round_scale",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("first", "", "", "", null, first),
        secondEffect = RoundCardEffect("second", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )

    @Test fun `immediate non Strike VP uses three point marginal value rather than fixed 60`() {
        val card = round(GameEffect.GAIN_ONE_VP)
        val vp = BattleMainPriority.score(context, card, BattleMainAction.RoundEffect1)
        val draw = BattleMainPriority.score(context, card, BattleMainAction.Draw)
        assertEquals(38, vp.total)
        assertEquals(45, draw.total)
        assertTrue(draw.total > vp.total)
        assertTrue(vp.adjustments.any { "non-Strike" in it.reason })
    }

    @Test fun `refresh and worms are not assigned unsupported fixed 55 premiums`() {
        for (effect in listOf(GameEffect.REFRESH_CREATURE, GameEffect.GAIN_TWO_WORMS)) {
            val value = BattleMainPriority.score(context, round(effect), BattleMainAction.RoundEffect1)
            assertEquals(35, value.total)
            assertTrue(value.adjustments.any { "not yet projected" in it.reason })
        }
    }

    @Test fun `Round and Draw use the same score path across Main stages`() {
        val card = round(GameEffect.GAIN_ONE_VP)
        for (action in listOf(BattleMainAction.Draw, BattleMainAction.RoundEffect1, BattleMainAction.RoundEffect2)) {
            assertEquals(
                BattleFirstMainPriority()(context, card, action).total,
                BattleMainPriority.score(context, card, action).total
            )
        }
    }
}
