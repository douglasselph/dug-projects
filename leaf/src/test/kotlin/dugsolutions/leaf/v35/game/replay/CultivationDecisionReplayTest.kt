package dugsolutions.leaf.v35.game.replay

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CultivationDecisionReplayTest {
    // Tests using a real RoundCard factory are covered by integration runs;
    // these check fork validation without exposing mutable game state.
    @Test fun rejectsPlantReplacementWithoutCardIdentity() {
        assertFailsWith<IllegalArgumentException> {
            CultivationReplayFork(1, PlayerId(1), 0, ReplayMainAction.ACTIVATE_PLANT)
        }
    }

    @Test fun stableKindsForValueActions() {
        assertEquals(ReplayMainAction.DRAW, CultivationMainAction.Draw.replayKind())
        assertEquals(ReplayMainAction.ROUND_EFFECT_1, CultivationMainAction.RoundEffect1.replayKind())
        assertEquals(ReplayMainAction.ROUND_EFFECT_2, CultivationMainAction.RoundEffect2.replayKind())
    }
}
