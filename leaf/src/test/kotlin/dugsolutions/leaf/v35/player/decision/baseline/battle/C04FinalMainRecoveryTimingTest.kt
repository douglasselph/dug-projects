package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Actual Final Main consumes the remaining Support window; open rows do not make support legal afterward. */
class C04FinalMainRecoveryTimingTest {
    private val self = PlayerId(0)
    private val rival = PlayerId(1)

    private fun context(): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        grove = DecisionContext.EMPTY.grove.copy(worms = 2),
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(id = self)),
        battle = BattleView(
            playerOrder = listOf(self, rival),
            rows = StrikeRow.entries.map { row -> BattleRowView(
                row = row, closed = false, players = listOf(
                    BattlePlayerRowView(self, row, emptyList(), emptyList(), 0, 0, 0, false),
                    BattlePlayerRowView(rival, row, emptyList(), emptyList(), 0, 0, 0, false)
                )
            ) }
        )
    )

    private fun round(effect: GameEffect) = RoundCard(
        quantity = 1, name = "C04_recovery_timing", type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("first", "", "", "", null, effect),
        secondEffect = RoundCardEffect("second", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )

    @Test fun `worm resource may be useful before final Main but not after it in last Battle`() {
        val ctx = context()
        val card = round(GameEffect.GAIN_TWO_WORMS)
        val action = BattleMainAction.RoundEffect1
        assertTrue(BattleMainPriority.score(ctx, card, action).total > 35)
        assertEquals(0, BattleMainPriority.finalMainScore(ctx, card, action).total)
        assertTrue(BattleMainPriority.finalMainScore(ctx, card, action).adjustments.any {
            it.reason.contains("Support")
        })
    }

    @Test fun `unrelated immediate scoring retains same final Main value`() {
        val ctx = context()
        val card = round(GameEffect.GAIN_TWO_WORMS)
        assertEquals(
            BattleMainPriority.score(ctx, card, BattleMainAction.RoundEffect2).total,
            BattleMainPriority.finalMainScore(ctx, card, BattleMainAction.RoundEffect2).total
        )
    }
}
