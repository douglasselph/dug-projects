package dugsolutions.leaf.v35.player.decision.trace

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecisionReasoningTest {
    @Test
    fun render_preservesSelectedScoreBreakdown() {
        val reasoning = DecisionReasoning(
            choiceLabel = "Root Cause -> D20 showing 1",
            baseScore = 55,
            adjustments = listOf(
                DecisionReasoningAdjustment(+57, "Flip D20 1 to 20"),
                DecisionReasoningAdjustment(+20, "Reaches Flower-17 purchase tier")
            ),
            total = 132
        )

        assertEquals(
            "Root Cause -> D20 showing 1\n" +
                "55  Base score\n" +
                "+57  Flip D20 1 to 20\n" +
                "+20  Reaches Flower-17 purchase tier\n" +
                "Total  132",
            reasoning.render()
        )
    }

    @Test
    fun constructor_rejectsInconsistentTotal() {
        assertFailsWith<IllegalArgumentException> {
            DecisionReasoning(
                choiceLabel = "choice",
                baseScore = 10,
                adjustments = listOf(
                    DecisionReasoningAdjustment(+5, "bonus")
                ),
                total = 99
            )
        }
    }
}
