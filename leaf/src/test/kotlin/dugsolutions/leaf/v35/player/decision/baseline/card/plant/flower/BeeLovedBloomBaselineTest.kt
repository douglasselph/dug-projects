package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BeeLovedBloomBaselineTest {
    @Test
    fun `influence favors Bee acquisition and grows with existing Bees`() {
        val candidate = DecisionCandidate("bee", PriorityScore(50), setOf(DecisionTag.ACQUIRE_BEE))
        val noBees = DecisionContext.EMPTY
        val twoBees = DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(bees = 2)
            )
        )

        val first = BeeLovedBloomBaseline.adjustments(noBees, candidate).sumOf { it.amount }
        val established = BeeLovedBloomBaseline.adjustments(twoBees, candidate).sumOf { it.amount }

        assertEquals(30, first)
        assertTrue(established > first)
    }
}
