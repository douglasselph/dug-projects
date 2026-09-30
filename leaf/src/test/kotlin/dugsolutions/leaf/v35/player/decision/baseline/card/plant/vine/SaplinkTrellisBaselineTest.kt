package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaplinkTrellisBaselineTest {
    @Test
    fun `ownership influence favors Root and Vine acquisition but not Flower`() {
        val context = DecisionContext.EMPTY
        val root = DecisionCandidate("root", PriorityScore(50), setOf(DecisionTag.ACQUIRE_ROOT))
        val vine = DecisionCandidate("vine", PriorityScore(50), setOf(DecisionTag.ACQUIRE_VINE))
        val flower = DecisionCandidate("flower", PriorityScore(50), setOf(DecisionTag.ACQUIRE_FLOWER))

        val rootAdjustment = SaplinkTrellisBaseline.adjustments(context, root).sumOf { it.amount }
        val vineAdjustment = SaplinkTrellisBaseline.adjustments(context, vine).sumOf { it.amount }

        assertTrue(rootAdjustment > 0)
        assertEquals(rootAdjustment, vineAdjustment)
        assertEquals(0, SaplinkTrellisBaseline.adjustments(context, flower).sumOf { it.amount })
    }
}
