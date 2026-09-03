package dugsolutions.leaf.v35.player.creature

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CreatureCardIdTest {

    @Test
    fun idsWithSameValue_areEqual() {
        assertEquals(
            CreatureCardId(3),
            CreatureCardId(3)
        )
    }

    @Test
    fun idsWithDifferentValues_areNotEqual() {
        assertNotEquals(
            CreatureCardId(3),
            CreatureCardId(4)
        )
    }
}
