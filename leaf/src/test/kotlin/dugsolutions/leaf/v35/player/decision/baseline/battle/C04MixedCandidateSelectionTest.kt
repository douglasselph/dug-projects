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

/** C04: compare *actual* Main scorer totals across different candidate families. */
class C04MixedCandidateSelectionTest {
    private val actor = PlayerId(0)
    private val rival = PlayerId(1)

    @Test fun `available Wisp competes with Draw but exhausted Wisp must lose`() {
        val round = round(GameEffect.GAIN_ONE_WISP)
        val available = context(4, 3)
        val exhausted = context(4, 0)
        val draw = BattleMainPriority.score(available, round, BattleMainAction.Draw)
        val gain = BattleMainPriority.score(available, round, BattleMainAction.RoundEffect1)
        val blocked = BattleMainPriority.score(exhausted, round, BattleMainAction.RoundEffect1)
        assertEquals(39, draw.total) // 35 + round(2.5 pips + 5/4 Wisp option)
        assertEquals(40, gain.total)
        assertEquals(0, blocked.total)
        assertTrue(gain.total > draw.total)
        assertTrue(draw.total > blocked.total)
    }

    @Test fun `unavailable opponent transfer does not outrank legal Draw`() {
        val round = round(GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT)
        val none = context(4, 0)
        val draw = BattleMainPriority.score(none, round, BattleMainAction.Draw)
        val steal = BattleMainPriority.score(none, round, BattleMainAction.RoundEffect1)
        assertEquals(0, steal.total)
        assertTrue(draw.total > steal.total)
    }

    @Test fun `Strike projection raises Draw ranking with same intrinsic draw`() {
        val round = round(GameEffect.GAIN_ONE_WISP)
        val bare = context(20, 0)
        val contested = bare.copy(battle = BattleView(
            playerOrder = listOf(actor, rival),
            rows = listOf(BattleRowView(StrikeRow.TOP, false, listOf(
                BattlePlayerRowView(actor, StrikeRow.TOP, emptyList(), emptyList(), 5, 0, 5, false),
                BattlePlayerRowView(rival, StrikeRow.TOP, emptyList(), emptyList(), 7, 0, 7, false)
            )))
        ))
        val withoutRow = BattleMainPriority.score(bare, round, BattleMainAction.Draw)
        val withRow = BattleMainPriority.score(contested, round, BattleMainAction.Draw)
        assertEquals(46, withoutRow.total)
        assertTrue(withRow.total > withoutRow.total)
        assertEquals(withRow.total,
            BattleFirstMainPriority()(contested, round, BattleMainAction.Draw).total)
    }

    private fun context(sides: Int, wisps: Int) = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        grove = DecisionContext.EMPTY.grove.copy(wispDeckRemaining = wisps),
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            id = actor,
            supply = listOf(DieView(0, sides, 1))
        ))
    )

    private fun round(first: GameEffect) = RoundCard(
        quantity = 1,
        name = "C04_mixed_candidate",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("first", "", "", "", null, first),
        secondEffect = RoundCardEffect("second", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )
}
