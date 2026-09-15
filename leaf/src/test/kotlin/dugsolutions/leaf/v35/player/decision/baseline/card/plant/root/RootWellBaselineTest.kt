package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RootWellBaselineTest {
    @Test
    fun `influence values Water only until useful reserve is reached`() {
        val candidate = DecisionCandidate("water", PriorityScore(50), setOf(DecisionTag.ACQUIRE_WATER))
        val empty = DecisionContext.EMPTY
        val full = DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(water = 2)
            )
        )

        assertEquals(30, RootWellBaseline.adjustments(empty, candidate).sumOf { it.amount })
        assertEquals(0, RootWellBaseline.adjustments(full, candidate).sumOf { it.amount })
    }
}
