package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class C04ResourceUtilityParityTest {
    private val empty = DecisionContext.EMPTY.copy(phase = RoundCardType.BATTLE)

    @Test fun `future Discard die cannot score immediate Strike or final round utility`() {
        val last = empty.copy(progress = empty.progress.copy(battleRoundsRemaining = 0))
        assertEquals(0, BattleRoundEffectPriority.score(last, GameEffect.GAIN_D20_TO_DISCARD).total)
        assertEquals(0, BattleResourceUtility.futureDiscardDie(last, 20))
    }

    @Test fun `future Discard value is discounted and bounded by immediate expected pips`() {
        val one = empty.copy(progress = empty.progress.copy(battleRoundsRemaining = 1))
        val two = empty.copy(progress = empty.progress.copy(battleRoundsRemaining = 2))
        val gainOne = BattleResourceUtility.futureDiscardDie(one, 20)
        val gainTwo = BattleResourceUtility.futureDiscardDie(two, 20)
        assertTrue(gainOne > 0)
        assertTrue(gainTwo >= gainOne)
        assertTrue(gainTwo <= 11)
        assertEquals(35 + gainTwo, BattleRoundEffectPriority.score(two, GameEffect.GAIN_D20_TO_DISCARD).total)
    }

    @Test fun `unavailable Wisp and nonexistent opponent transfer do not inherit base priority`() {
        assertEquals(0, BattleRoundEffectPriority.score(empty, GameEffect.GAIN_ONE_WISP).total)
        assertEquals(0, BattleRoundEffectPriority.score(empty, GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS).total)
    }

    @Test fun `immediate VP and zero value Round effects do not pretend to be Strike transitions`() {
        assertEquals(38, BattleRoundEffectPriority.score(empty, GameEffect.GAIN_ONE_VP).total)
        assertEquals(35, BattleRoundEffectPriority.score(empty, GameEffect.GAIN_TWO_WORMS).total)
    }
}
