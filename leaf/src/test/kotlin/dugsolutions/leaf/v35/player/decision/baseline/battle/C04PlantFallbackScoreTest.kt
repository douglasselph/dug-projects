package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class C04PlantFallbackScoreTest {
    @Test fun `legacy card base does not affect fallback comparison`() {
        val low = BattlePlantFallbackScore.normalize(PriorityScore(45).adjusted(12, "Same effect"))
        val high = BattlePlantFallbackScore.normalize(PriorityScore(95).adjusted(12, "Same effect"))
        assertEquals(47, low.total)
        assertEquals(low, high)
    }

    @Test fun `fallback preserves negative and positive context adjustments`() {
        val score = BattlePlantFallbackScore.normalize(
            PriorityScore(90).adjusted(8, "Benefit").adjusted(-17, "Unavailable resource")
        )
        assertEquals(26, score.total)
        assertEquals(2, score.adjustments.size)
    }

    @Test fun `research no op disqualification is retained`() {
        val noOp = PriorityScore(-10_000).adjusted(0, "Research no-op")
        assertEquals(noOp, BattlePlantFallbackScore.normalize(noOp))
    }
}
