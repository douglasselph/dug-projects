package dugsolutions.leaf.v30.wisp

import dugsolutions.leaf.v30.common.Commons
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WispCardRegistryTest {

    @Test
    fun loadFromCsv_whenFileDoesNotExist_throwsException() {
        // Arrange
        val registry = WispCardRegistry()

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            registry.loadFromCsv("missing-wisps.csv")
        }
    }

    @Test
    fun loadFromCsv_withWispList_loadsCards() {
        // Arrange
        val registry = WispCardRegistry()

        // Act
        registry.loadFromCsv(Commons.WISP_LIST)

        // Assert
        assertEquals(9, registry.getAllCards().size)
    }

    @Test
    fun loadFromCsv_withWispList_parsesKnownCard() {
        // Arrange
        val registry = WispCardRegistry()

        // Act
        registry.loadFromCsv(Commons.WISP_LIST)
        val card = registry.getCard("Wisp_Award_VP")

        // Assert
        assertNotNull(card)
        assertEquals(3, card.quantity)
        assertEquals("Wisp_Award_VP", card.name)
        assertEquals("Wisp of Honor", card.title)
        assertEquals(3, card.count)
        assertEquals("Keep: 2 VP (END)", card.effect)
        assertEquals("images/victory_victory.png", card.lineIcons)
        assertEquals(80, card.lineIconsHeight)
        assertEquals("images/cloud_honor.png", card.mainBackdrop)
    }

    @Test
    fun loadFromCsv_withWispList_preservesMultilineEffects() {
        // Arrange
        val registry = WispCardRegistry()

        // Act
        registry.loadFromCsv(Commons.WISP_LIST)
        val card = registry.getCard("Wisp_Gain_Critters")

        // Assert
        assertNotNull(card)
        assertTrue(card.effect.lines().size > 1)
        assertTrue(card.effect.contains("Gain any 2 critters"))
    }

    @Test
    fun getCard_whenCardDoesNotExist_returnsNull() {
        // Arrange
        val registry = WispCardRegistry()
        registry.loadFromCsv(Commons.WISP_LIST)

        // Act
        val result = registry.getCard("MissingWisp")

        // Assert
        assertNull(result)
    }

    @Test
    fun loadFromCsv_allCardsAreUnique() {
        // Arrange
        val registry = WispCardRegistry()

        // Act
        registry.loadFromCsv(Commons.WISP_LIST)
        val cards = registry.getAllCards()
        val duplicates = cards.map { it.id }.groupBy { it }.filter { it.value.size > 1 }.keys

        // Assert
        assertTrue(duplicates.isEmpty(), "Found duplicate wisp card IDs: $duplicates")
    }
}
