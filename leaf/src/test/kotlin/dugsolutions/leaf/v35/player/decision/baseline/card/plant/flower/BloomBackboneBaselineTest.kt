package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BloomBackboneBaselineTest {
    @Test
    fun `ownership influence favors Vine and Flower acquisition but not Root`() {
        val context = DecisionContext.EMPTY
        val root = DecisionCandidate("root", PriorityScore(50), setOf(DecisionTag.ACQUIRE_ROOT))
        val vine = DecisionCandidate("vine", PriorityScore(50), setOf(DecisionTag.ACQUIRE_VINE))
        val flower = DecisionCandidate("flower", PriorityScore(50), setOf(DecisionTag.ACQUIRE_FLOWER))

        val vineAdjustment = BloomBackboneBaseline.adjustments(context, vine).sumOf { it.amount }
        val flowerAdjustment = BloomBackboneBaseline.adjustments(context, flower).sumOf { it.amount }

        assertTrue(vineAdjustment > 0)
        assertEquals(vineAdjustment, flowerAdjustment)
        assertEquals(0, BloomBackboneBaseline.adjustments(context, root).sumOf { it.amount })
    }
}
