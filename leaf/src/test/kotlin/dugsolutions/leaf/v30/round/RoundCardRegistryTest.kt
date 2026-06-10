package dugsolutions.leaf.v30.round

import dugsolutions.leaf.v30.common.Commons
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RoundCardRegistryTest {

    @Test
    fun loadFromCsv_whenFileDoesNotExist_throwsException() {
        val registry = RoundCardRegistry()

        assertThrows<IllegalArgumentException> {
            registry.loadFromCsv("missing-round-cards.csv")
        }
    }

    @Test
    fun loadFromCsv_withRoundCardList_loadsCards() {
        val registry = RoundCardRegistry()

        registry.loadFromCsv(Commons.ROUND_CARD_LIST)

        assertEquals(12, registry.getAllCards().size)
    }

    @Test
    fun loadFromCsv_withRoundCardList_parsesKnownCard() {
        val registry = RoundCardRegistry()

        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        val card = registry.getCard("Battle_Bloom_Burrow")

        assertNotNull(card)
        assertEquals(1, card.quantity)
        assertEquals("Battle_Bloom_Burrow", card.name)
        assertEquals("Battle", card.title)
        assertEquals("Bloom", card.effect1Title)
        assertEquals("Gain 1 VP", card.effect1Text)
        assertEquals("b80000", card.effect1Bg)
        assertEquals("f8f5f2", card.effect1TextFg)
        assertEquals("images/turn_bloom.png", card.effect1Image)
        assertEquals("images/victory.png", card.effect1Icon)
        assertEquals("Burrow", card.effect2Title)
        assertEquals("Gain Worm", card.effect2Text)
        assertEquals("7a0000", card.effect2Bg)
        assertEquals("f8f5f2", card.effect2TextFg)
        assertEquals("images/turn_surge_worm.png", card.effect2Image)
        assertEquals("images/ic_token_worm.png", card.effect2Icon)
        assertEquals("images/battle_transition_back.png", card.backImage)
    }

    @Test
    fun loadFromCsv_withRoundCardList_preservesMultilineEffects() {
        val registry = RoundCardRegistry()

        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        val card = registry.getCard("Resource_Compost_Mulch")

        assertNotNull(card)
        assertTrue(card.effect2Text.lines().size > 1)
        assertTrue(card.effect2Text.contains("Store a die"))
    }

    @Test
    fun loadFromCsv_withRoundCardList_parsesEmptyImageFieldsAsNull() {
        val registry = RoundCardRegistry()

        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        val card = registry.getCard("Resource_Compost_Mulch")

        assertNotNull(card)
        assertNull(card.effect1Icon)
        assertNotNull(card.effect2Icon)
    }

    @Test
    fun getCard_whenCardDoesNotExist_returnsNull() {
        val registry = RoundCardRegistry()
        registry.loadFromCsv(Commons.ROUND_CARD_LIST)

        val result = registry.getCard("MissingRoundCard")

        assertNull(result)
    }

    @Test
    fun loadFromCsv_allCardsAreUnique() {
        val registry = RoundCardRegistry()

        registry.loadFromCsv(Commons.ROUND_CARD_LIST)
        val cards = registry.getAllCards()
        val duplicates = cards.map { it.id }.groupBy { it }.filter { it.value.size > 1 }.keys

        assertTrue(duplicates.isEmpty(), "Found duplicate round card IDs: $duplicates")
    }
}
