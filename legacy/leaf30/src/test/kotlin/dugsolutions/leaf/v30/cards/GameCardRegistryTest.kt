package dugsolutions.leaf.v30.cards

import dugsolutions.leaf.v30.cards.domain.CardType
import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.common.Commons
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameCardRegistryTest {

    private lateinit var cardRegistry: GameCardRegistry

    @BeforeEach
    fun setup() {
        cardRegistry = GameCardRegistry()
    }

    @Test
    fun loadFromCsv_whenFileDoesNotExist_throwsException() {
        // Arrange
        val nonexistentFile = "nonexistent.csv"

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            cardRegistry.loadFromCsv(nonexistentFile)
        }
    }

    @Test
    fun loadFromCsv_withCardList_loadsCards() {
        // Act
        cardRegistry.loadFromCsv(Commons.CARD_LIST)

        // Assert
        assertTrue(cardRegistry.getAllCards().isNotEmpty())
    }

    @Test
    fun loadFromCsv_withCardList_parsesKnownCard() {
        // Act
        cardRegistry.loadFromCsv(Commons.CARD_LIST)
        val card = cardRegistry.getCard("Root_05_01")

        // Assert
        assertNotNull(card)
        assertEquals(8, card.quantity)
        assertEquals("Root_05_01", card.name)
        assertEquals("Root Bulwark", card.title)
        assertEquals(CardType.ROOT, card.type)
        assertEquals(5, card.cost)
        assertEquals("images/battle_only.png", card.lineIcon)
        assertEquals("images/root_bulwark_full.png", card.fullImage)
        assertEquals("images/back_root2.png", card.bgImage2)
        assertEquals("images/cf_root_bulwark.png", card.bgCardImage2)
        assertEquals(CardEffect.PLACE_BULWARK_TOKEN, card.effect)
    }

    @Test
    fun loadFromCsv_withCardList_convertsKnownEffects() {
        // Act
        cardRegistry.loadFromCsv(Commons.CARD_LIST)

        // Assert
        assertEquals(CardEffect.RAISE_DIE_PLUS_1_AND_GAIN_WATER, cardRegistry.getCard("Root_07_01")?.effect)
        assertEquals(CardEffect.GAIN_D4_OR_RETURN_D4_RAISE_DIE_PLUS_4, cardRegistry.getCard("Flower_11_03")?.effect)
        assertEquals(CardEffect.DRAW_TWO_DICE, cardRegistry.getCard("Flower_17_04")?.effect)
        assertEquals(CardEffect.RAISE_DIE_PLUS_4, cardRegistry.getCard("Vine_07_04")?.effect)
        assertEquals(CardEffect.RESOLVE_GRAFTED_ROOT_OR_VINE_EFFECT, cardRegistry.getCard("Vine_09_01")?.effect)
    }

    @Test
    fun loadFromCsv_withCardList_parsesEmptyImageFieldsAsNull() {
        // Act
        cardRegistry.loadFromCsv(Commons.CARD_LIST)
        val card = cardRegistry.getCard("Root_05_04")

        // Assert
        assertNotNull(card)
        assertNull(card.lineIcon)
        assertNotNull(card.fullImage)
        assertNotNull(card.bgImage2)
        assertNotNull(card.bgCardImage2)
    }

    @Test
    fun getCard_whenCardDoesNotExist_returnsNull() {
        // Act
        val result = cardRegistry.getCard("NonExistentCard")

        // Assert
        assertNull(result)
    }

    @Test
    fun getAllCards_whenRegistryEmpty_returnsEmptyList() {
        // Act
        val result = cardRegistry.getAllCards()

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun loadFromCsv_allCardsAreUnique() {
        // Act
        cardRegistry.loadFromCsv(Commons.CARD_LIST)
        val cards = cardRegistry.getAllCards()

        // Assert
        val ids = cards.map { it.id }
        val duplicates = ids.groupBy { it }
            .filter { it.value.size > 1 }
            .keys

        assertTrue(duplicates.isEmpty(), "Found duplicate card IDs: $duplicates")
    }

}
