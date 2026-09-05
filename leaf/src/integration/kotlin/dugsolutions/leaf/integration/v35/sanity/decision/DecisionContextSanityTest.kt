package dugsolutions.leaf.integration.v35.sanity.decision

import dugsolutions.leaf.integration.v35.sanity.cultivation.cultivationHarness
import dugsolutions.leaf.integration.v35.sanity.cultivation.finishBuildWithWater
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DecisionContextSanityTest {

    @Test
    fun `production decisions receive fresh immutable game views`() {
        val contexts = mutableListOf<DecisionContext>()
        val first = ScriptedDecisionDirector().apply {
            cultivation.thenChoose { request ->
                contexts += request.context
                CultivationAction.Main(CultivationMainAction.RoundEffect1)
            }
            cultivation.thenChoose { request ->
                contexts += request.context
                CultivationAction.Main(CultivationMainAction.RoundEffect1)
            }
            cultivation.thenDone()
        }
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(first = first, second = second).use { harness ->
            harness.revealNextRound()
            harness.runCultivationBuildActions()

            assertEquals(2, contexts.size)
            val beforeFirstAction = contexts[0]
            val beforeSecondAction = contexts[1]

            assertFalse(beforeFirstAction === DecisionContext.EMPTY)
            assertEquals(RoundCardType.CULTIVATION, beforeFirstAction.phase)
            assertEquals(1, beforeFirstAction.progress.currentCultivationRoundNumber)
            assertEquals(0, beforeFirstAction.self.board.water)

            // The first Round Effect gains Water. The next decision receives a
            // newly-created snapshot while the previous snapshot stays frozen.
            assertEquals(0, beforeFirstAction.self.board.water)
            assertEquals(1, beforeSecondAction.self.board.water)
            assertEquals(2, harness.snapshot().player(1).water)

            first.assertExhausted()
            second.assertExhausted()
        }
    }
}
