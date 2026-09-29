package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ForgetMeNotCalibrationTest {
    @Test
    fun `high sided distant discard die is preferred before Battle`() {
        val base = DecisionContext.EMPTY
        val d8 = DieView(0, 8, 3)
        val d20 = DieView(1, 20, 4)
        val context = base.copy(
            phase = RoundCardType.CULTIVATION,
            progress = base.progress.copy(upcomingRoundTypes = listOf(RoundCardType.BATTLE)),
            self = base.self.copy(board = base.self.board.copy(
                supply = listOf(DieView(2, 4, 2), DieView(3, 6, 3)),
                discard = listOf(d8, d20)
            ))
        )
        fun score(die: DieView) = CardScoringHelpers.scoreDieTarget(
            GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND,
            context,
            EffectDieChoice(die.index, die.sides, die.value)
        ).total

        assertTrue(score(d20) > score(d8))
        assertTrue(score(d8) > 50, "D8 should still be a meaningful recovery candidate")
    }
}
