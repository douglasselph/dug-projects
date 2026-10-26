package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Multirow state reversals, using the actual Battle Main resource scorer. */
class C04RecoveryOpportunityMatrixTest {
    private val player = PlayerId(0)
    private val rival = PlayerId(1)
    private val base = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            id = player, supply = listOf(DieView(0, 4, 1))
        ))
    )

    private fun rows(closed: Boolean, done: Boolean = false) = base.copy(
        battle = BattleView(
            playerOrder = listOf(player, rival),
            donePlayerIds = if (done) setOf(player) else emptySet(),
            rows = StrikeRow.entries.map { row ->
                BattleRowView(row = row, closed = closed, players = listOf(
                    BattlePlayerRowView(player, row, emptyList(), emptyList(), 0, 0, 0, false),
                    BattlePlayerRowView(rival, row, emptyList(), emptyList(), 0, 0, 0, false)
                ))
            }
        )
    )

    @Test fun `worm resource turns from unavailable to useful when a row opens`() {
        val closed = rows(closed = true).copy(grove = base.grove.copy(worms = 2))
        val open = rows(closed = false).copy(grove = base.grove.copy(worms = 2))
        val noSupply = open.copy(grove = open.grove.copy(worms = 0))
        val effect = GameEffect.GAIN_TWO_WORMS
        assertEquals(0, BattleRoundEffectPriority.score(closed, effect).total)
        assertTrue(BattleRoundEffectPriority.score(open, effect).total > 35)
        assertEquals(0, BattleRoundEffectPriority.score(noSupply, effect).total)
        assertEquals(0, BattleRoundEffectPriority.score(rows(false, done = true).copy(grove = open.grove), effect).total)
    }

    @Test fun `resource becomes better than a zero utility Round choice only when usable`() {
        val open = rows(false).copy(grove = base.grove.copy(worms = 2))
        val exhausted = open.copy(grove = open.grove.copy(worms = 0))
        val worm = GameEffect.GAIN_TWO_WORMS
        val unavailableWisp = GameEffect.GAIN_ONE_WISP
        assertTrue(BattleRoundEffectPriority.score(open, worm).total >
            BattleRoundEffectPriority.score(open, unavailableWisp).total)
        assertEquals(BattleRoundEffectPriority.score(exhausted, worm).total,
            BattleRoundEffectPriority.score(exhausted, unavailableWisp).total)
    }
}
