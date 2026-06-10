package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.cards.domain.CardType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class GroveStackIDTest {

    @Test
    fun from_withMatchingTypeAndCost_returnsStackId() {
        assertEquals(GroveStackID.ROOT_5, GroveStackID.from(CardType.ROOT, 5))
        assertEquals(GroveStackID.FLOWER_14, GroveStackID.from(CardType.FLOWER, 14))
        assertEquals(GroveStackID.VINE_11, GroveStackID.from(CardType.VINE, 11))
    }

    @Test
    fun from_whenNoMatchingTypeAndCost_throwsException() {
        assertThrows<IllegalArgumentException> {
            GroveStackID.from(CardType.ROOT, 11)
        }
    }

}
