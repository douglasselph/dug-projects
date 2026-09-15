package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VineCardScorersTest {
    @Test
    fun `vine phase values preserve important Battle differences`() {
        assertEquals(68, LowAndBeholdBaseline.cultivationPlayBase)
        assertTrue(PartingThornBaseline.battlePlayBase > PartingThornBaseline.cultivationPlayBase)
        assertTrue(VineAndPunishmentBaseline.battlePlayBase > VineAndPunishmentBaseline.cultivationPlayBase)
    }
}
