package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoning
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningAdjustment
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningAlternative
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChronicleDecisionReasoningSinkTest {
    @Test
    fun record_writesTypedImmutableDecisionReasoningEntry() {
        val chronicle = GameChronicle()
        val sink = ChronicleDecisionReasoningSink(
            chronicle = chronicle,
            playerId = PlayerId(2)
        )

        sink.record(
            DecisionReasoning(
                choiceLabel = "WORM",
                baseScore = 50,
                adjustments = listOf(
                    DecisionReasoningAdjustment(+25, "Root Appreciation makes Worms more valuable")
                ),
                total = 75
            )
        )

        val entry = chronicle.entries.single() as GameEntry.DecisionReasoning
        assertEquals(PlayerId(2), entry.playerId)
        assertEquals("WORM", entry.choiceLabel)
        assertEquals(50, entry.baseScore)
        assertEquals(75, entry.total)
        assertEquals(
            listOf("Root Appreciation makes Worms more valuable"),
            entry.adjustments.map { it.reason }
        )
    }
    @Test
    fun record_preservesAlternativesAndCalibrationObservations() {
        val chronicle = GameChronicle()
        val sink = ChronicleDecisionReasoningSink(chronicle, PlayerId(1))
        val draw = DecisionReasoningAlternative("Draw", 53, emptyList(), 53, false, mapOf("drawUtility" to "53"))
        // Keep totals internally consistent: use an adjustment for the selected target.
        val target = DecisionReasoningAlternative(
            "Mulch", 45, listOf(DecisionReasoningAdjustment(8, "Battle is next")), 53, true,
            mapOf("targetDieSides" to "20", "targetDieValue" to "5")
        )
        sink.record(
            DecisionReasoning(
                choiceLabel = "Mulch", baseScore = 45,
                adjustments = target.adjustments, total = 53,
                alternatives = listOf(draw, target),
                observations = target.observations
            )
        )
        val entry = chronicle.entries.single() as GameEntry.DecisionReasoning
        assertEquals(2, entry.alternatives.size)
        assertEquals("20", entry.observations["targetDieSides"])
        assertEquals("5", entry.alternatives.single { it.selected }.observations["targetDieValue"])
    }

}
