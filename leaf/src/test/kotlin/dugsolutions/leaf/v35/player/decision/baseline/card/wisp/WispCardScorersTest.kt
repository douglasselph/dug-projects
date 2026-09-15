package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WispCardScorersTest {
    @Test
    fun `pollinating scorer covers all colors and battle wisps remain phase weighted`() {
        assertEquals(4, PollinatingWispBaseline.cardNames.size)
        assertTrue(PollenTheftBaseline.battlePlayBase > PollenTheftBaseline.cultivationPlayBase)
        assertTrue(WispsLastWordBaseline.battlePlayBase > WispsLastWordBaseline.cultivationPlayBase)
    }
}
