package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlowerCardScorersTest {
    @Test
    fun `flower base values preserve high value and phase sensitive cards`() {
        assertEquals(85, OEdelweissBaseline.cultivationPlayBase)
        assertEquals(82, QueensBlossomBaseline.cultivationPlayBase)
        assertTrue(SappingSnapdragonBaseline.battlePlayBase > SappingSnapdragonBaseline.cultivationPlayBase)
        assertTrue(BloomBackflipBaseline.battlePlayBase > BloomBackflipBaseline.cultivationPlayBase)
    }
}
