package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RootCardScorersTest {
    @Test
    fun `root base priorities match Human Baseline calibration`() {
        assertEquals(55, RootDoubleDownBaseline.cultivationPlayBase)
        assertEquals(75, RootAwakeningBaseline.cultivationPlayBase)
        assertTrue(RootWellBaseline.battlePlayBase > RootWellBaseline.cultivationPlayBase)
    }
}
