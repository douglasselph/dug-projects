package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class C04RoundEffectContextTest {
    @Test fun `empty Wisp deck has no gain value`() {
        val context = DecisionContext.EMPTY
        val blocked = BattleRoundEffectPriority.score(context, GameEffect.GAIN_ONE_WISP)
        val available = BattleRoundEffectPriority.score(context.copy(
            grove = context.grove.copy(wispDeckRemaining = 3)), GameEffect.GAIN_ONE_WISP)
        assertTrue(available.total > blocked.total)
        assertEquals(0, blocked.total)
    }

    @Test fun `cannot steal from opponents without Wisps`() {
        val context = DecisionContext.EMPTY
        assertEquals(0, BattleRoundEffectPriority.score(
            context, GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT).total)
        val opponent = OpponentView(context.self.board.copy(id = PlayerId(1)), wispCount = 2)
        val eligible = context.copy(opponents = listOf(opponent))
        assertTrue(BattleRoundEffectPriority.score(
            eligible, GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT).total > 0)
    }

    @Test fun `discard die cannot affect current final Battle`() {
        val context = DecisionContext.EMPTY
        assertEquals(0, BattleRoundEffectPriority.score(
            context, GameEffect.GAIN_D20_TO_DISCARD).total)
        val earlier = context.copy(progress = context.progress.copy(roundsRemainingToReveal = 2))
        assertTrue(BattleRoundEffectPriority.score(
            earlier, GameEffect.GAIN_D20_TO_DISCARD).total > 0)
    }
}
