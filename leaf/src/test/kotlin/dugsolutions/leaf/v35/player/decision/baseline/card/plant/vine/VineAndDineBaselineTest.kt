package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VineAndDineBaselineTest {
    @Test
    fun `ownership influence preserves the last usable critter from unrelated spending`() {
        val context = context(bees = 1, worms = 0)
        val spendBee = DecisionCandidate("spend bee", PriorityScore(50), setOf(DecisionTag.SPEND_BEE))

        val adjustment = VineAndDineBaseline.adjustments(context, spendBee).sumOf { it.amount }

        assertTrue(adjustment < 0)
    }

    @Test
    fun `ownership influence does not discourage spending the critter through Vine and Dine itself`() {
        val context = context(bees = 0, worms = 1)
        val useCard = DecisionCandidate(
            "use Vine and Dine",
            PriorityScore(50),
            setOf(DecisionTag.SPEND_WORM, DecisionTag.SPEND_CRITTER_FOR_VINE_AND_DINE)
        )

        assertEquals(0, VineAndDineBaseline.adjustments(context, useCard).sumOf { it.amount })
    }

    @Test
    fun `ownership influence values acquiring a usable critter when none is available`() {
        val context = context(bees = 0, worms = 0)
        val acquireBee = DecisionCandidate("gain bee", PriorityScore(50), setOf(DecisionTag.ACQUIRE_BEE))
        val acquireWorm = DecisionCandidate("gain worm", PriorityScore(50), setOf(DecisionTag.ACQUIRE_WORM))

        assertTrue(VineAndDineBaseline.adjustments(context, acquireBee).sumOf { it.amount } > 0)
        assertTrue(VineAndDineBaseline.adjustments(context, acquireWorm).sumOf { it.amount } > 0)
    }

    @Test
    fun `ownership influence allows spending one critter while another remains`() {
        val context = context(bees = 1, worms = 1)
        val spendBee = DecisionCandidate("spend bee", PriorityScore(50), setOf(DecisionTag.SPEND_BEE))

        assertEquals(0, VineAndDineBaseline.adjustments(context, spendBee).sumOf { it.amount })
    }

    private fun context(bees: Int, worms: Int): DecisionContext =
        DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(bees = bees, worms = worms)
            )
        )
}
