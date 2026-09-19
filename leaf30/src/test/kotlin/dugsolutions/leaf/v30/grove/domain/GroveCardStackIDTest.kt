package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.cards.domain.CardType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class GroveCardStackIDTest {

    @Test
    fun from_withMatchingTypeAndCost_returnsStackId() {
        assertEquals(GroveCardStackID.ROOT_5, GroveCardStackID.from(CardType.ROOT, 5))
        assertEquals(GroveCardStackID.FLOWER_14, GroveCardStackID.from(CardType.FLOWER, 14))
        assertEquals(GroveCardStackID.VINE_11, GroveCardStackID.from(CardType.VINE, 11))
    }

    @Test
    fun from_whenNoMatchingTypeAndCost_throwsException() {
        assertThrows<IllegalArgumentException> {
            GroveCardStackID.from(CardType.ROOT, 11)
        }
    }

}
