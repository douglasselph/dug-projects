package dugsolutions.leaf.simulation.v35.learning.cultivation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CultivationMainResearchTest {
    @Test
    fun `affected role rotates across configured player count`() {
        assertEquals(listOf(0,1,0,1,0,1), (0 until 6).map { affectedSeat(it, 2) })
        assertEquals(listOf(0,1,2,0,1,2), (0 until 6).map { affectedSeat(it, 3) })
        assertEquals(listOf(0,1,2,3,0,1,2,3), (0 until 8).map { affectedSeat(it, 4) })
    }

    @Test
    fun `training options retain explicit research controls`() {
        val o = CultivationTrainOptions.parse(listOf(
            "--games", "7",
            "--players", "3",
            "--rounds", "2/2/3",
            "--random-grove",
            "--plant-overrides", "data/research/resync/resync-current.csv",
            "--round-overrides", "data/research/round-overrides/sunlight-token.csv",
            "--buy-policy", "human"
        ))
        assertEquals(7, o.games)
        assertEquals(3, o.players)
        assertEquals("2/2/3", o.roundLabel)
        assertEquals("000000000", o.grovePattern)
        assertEquals("human", o.buyPolicy)
    }

    @Test
    fun `evaluation options support learned Buy as an independent fixed policy`() {
        val o = CultivationEvalOptions.parse(listOf(
            "--games=9", "--players=2", "--buy-policy=learned",
            "--buy-weights=data/ai/example.weights"
        ))
        assertEquals(9, o.games)
        assertEquals(2, o.players)
        assertEquals("learned", o.buyPolicy)
        assertEquals("data/ai/example.weights", o.buyWeights.toString())
    }
}
