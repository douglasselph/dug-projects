package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoning
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningAdjustment
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
}
