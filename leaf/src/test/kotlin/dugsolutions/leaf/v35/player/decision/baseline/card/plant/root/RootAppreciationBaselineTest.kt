package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RootAppreciationBaselineTest {
    @Test
    fun `influence increases Worm acquisition but not Bee acquisition`() {
        val context = DecisionContext.EMPTY
        val worm = DecisionCandidate("worm", PriorityScore(50), setOf(DecisionTag.ACQUIRE_WORM))
        val bee = DecisionCandidate("bee", PriorityScore(50), setOf(DecisionTag.ACQUIRE_BEE))

        assertEquals(35, RootAppreciationBaseline.adjustments(context, worm).sumOf { it.amount })
        assertEquals(0, RootAppreciationBaseline.adjustments(context, bee).sumOf { it.amount })
    }
}
