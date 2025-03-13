package dugsolutions.leaf.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.tool.Randomizer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import io.mockk.mockk
import io.mockk.every

class GameCardIDsTest {
    private lateinit var cardManager: CardManager
    private lateinit var randomizer: Randomizer
    
    @BeforeEach
    fun setup() {
        cardManager = mockk(relaxed = true)
        randomizer = Randomizer.create(42) // Fixed seed for reproducibility
    }
    
    private fun createTestCardId(index: Int): CardID = GenCardID.generateId("test_card_$index")
    
    @Test
    fun isEmpty_whenNewWithEmptyList_returnsTrue() {
        // Arrange
        val cards = GameCardIDs(cardManager, emptyList(), randomizer)
        
        // Act
        val result = cards.isEmpty()
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun isEmpty_whenContainsCards_returnsFalse() {
        // Arrange
        val cards = GameCardIDs(cardManager, listOf(createTestCardId(1)), randomizer)
        
        // Act
        val result = cards.isEmpty()
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun size_whenNewWithEmptyList_returnsZero() {
        // Arrange
        val cards = GameCardIDs(cardManager, emptyList(), randomizer)
        
        // Act
        val result = cards.size
        
        // Assert
        assertEquals(0, result)
    }
    
    @Test
    fun size_whenContainsCards_returnsCorrectCount() {
        // Arrange
        val cards = GameCardIDs(cardManager, listOf(
            createTestCardId(1),
            createTestCardId(2),
            createTestCardId(3)
        ), randomizer)
        
        // Act
        val result = cards.size
        
        // Assert
        assertEquals(3, result)
    }
    
    @Test
    fun get_whenIndexInRange_returnsCorrectCard() {
        // Arrange
        val cardId = createTestCardId(42)
        val cards = GameCardIDs(cardManager, listOf(
            createTestCardId(1),
            cardId,
            createTestCardId(3)
        ), randomizer)
        
        // Act
        val result = cards[1]
        
        // Assert
        assertEquals(cardId, result)
    }
    
    @Test
    fun plus_whenCombiningTwoCollections_returnsAllCards() {
        // Arrange
        val cards1 = GameCardIDs(cardManager, listOf(createTestCardId(1), createTestCardId(2)), randomizer)
        val cards2 = GameCardIDs(cardManager, listOf(createTestCardId(3), createTestCardId(4)), randomizer)
        
        // Act
        val result = cards1 + cards2
        
        // Assert
        assertEquals(4, result.size)
        assertTrue(result.cardIds.containsAll(cards1.cardIds))
        assertTrue(result.cardIds.containsAll(cards2.cardIds))
    }
    
    @Test
    fun shuffle_withRandomizer_producesExpectedOrder() {
        // Arrange
        val originalCards = listOf(
            createTestCardId(1),
            createTestCardId(2),
            createTestCardId(3)
        )
        val cards = GameCardIDs(cardManager, originalCards, randomizer)
        
        // Act
        cards.shuffle()
        
        // Assert
        assertNotEquals(originalCards, cards.cardIds)
        assertEquals(originalCards.size, cards.size)
        assertTrue(cards.cardIds.containsAll(originalCards))
    }
    
    @Test
    fun draw_whenNotEmpty_returnsAndRemovesFirstCard() {
        // Arrange
        val firstCard = createTestCardId(1)
        val cards = GameCardIDs(cardManager, listOf(firstCard, createTestCardId(2)), randomizer)
        
        // Act
        val drawn = cards.draw()
        
        // Assert
        assertEquals(firstCard, drawn)
        assertEquals(1, cards.size)
        assertFalse(cards.cardIds.contains(firstCard))
    }
    
    @Test
    fun draw_whenEmpty_returnsNull() {
        // Arrange
        val cards = GameCardIDs(cardManager, emptyList(), randomizer)
        
        // Act
        val drawn = cards.draw()
        
        // Assert
        assertNull(drawn)
    }
    
    @Test
    fun reset_withNewCards_replacesExistingCards() {
        // Arrange
        val cards = GameCardIDs(cardManager, listOf(createTestCardId(1), createTestCardId(2)), randomizer)
        val newCards = listOf(
            createTestCardId(3),
            createTestCardId(4),
            createTestCardId(5)
        )
        
        // Act
        cards.reset(newCards)
        
        // Assert
        assertEquals(newCards.size, cards.size)
        assertTrue(cards.cardIds.containsAll(newCards))
    }
    
    @Test
    fun add_singleCard_appendsToCollection() {
        // Arrange
        val cards = GameCardIDs(cardManager, listOf(createTestCardId(1)), randomizer)
        val newCard = createTestCardId(2)
        
        // Act
        cards.add(newCard)
        
        // Assert
        assertEquals(2, cards.size)
        assertTrue(cards.cardIds.contains(newCard))
    }
    
    @Test
    fun addAll_multipleCards_appendsAllToCollection() {
        // Arrange
        val cards = GameCardIDs(cardManager, listOf(createTestCardId(1)), randomizer)
        val newCards = listOf(createTestCardId(2), createTestCardId(3))
        
        // Act
        cards.addAll(newCards)
        
        // Assert
        assertEquals(3, cards.size)
        assertTrue(cards.cardIds.containsAll(newCards))
    }
    
    @Test
    fun transfer_fromOtherCollection_movesAllCards() {
        // Arrange
        val cards = GameCardIDs(cardManager, listOf(createTestCardId(1)), randomizer)
        val source = GameCardIDs(cardManager, listOf(createTestCardId(2), createTestCardId(3)), randomizer)
        val sourceCards = source.cardIds.toList() // Copy for verification
        
        // Act
        cards.transfer(source)
        
        // Assert
        assertEquals(3, cards.size)
        assertTrue(cards.cardIds.containsAll(sourceCards))
        assertTrue(source.isEmpty())
    }

    @Test
    fun draw_withPreferredCount_respectsHandSize() {
        // Arrange
        val cards = GameCardIDs(cardManager, listOf(createTestCardId(1), createTestCardId(2)), randomizer)
        val beforeSize = cards.size

        // Act
        cards.draw()
        
        // Assert
        assertEquals(beforeSize-1, cards.size)
        assertTrue(cards.cardIds.containsAll(listOf(createTestCardId(2))))
    }

    @Test
    fun draw_withPreferredCount_fillsRemainingSpace() {
        // Arrange
        val cards = GameCardIDs(cardManager, listOf(createTestCardId(1)), randomizer)

        // Act
        cards.draw()
        
        // Assert
        assertEquals(0, cards.size)
    }

    @Test
    fun draw_withPreferredCount_handlesEmptySupply() {
        // Arrange
        val cards = GameCardIDs(cardManager, emptyList(), randomizer)

        // Act
        cards.draw()
        
        // Assert
        assertEquals(0, cards.size)
    }

    @Test
    fun removeTop_whenNotEmpty_returnsAndRemovesFirstCard() {
        // Arrange
        val firstCard = createTestCardId(1)
        val cards = GameCardIDs(cardManager, listOf(firstCard, createTestCardId(2)), randomizer)
        
        // Act
        val removed = cards.removeTop()
        
        // Assert
        assertEquals(firstCard, removed)
        assertEquals(1, cards.size)
        assertFalse(cards.cardIds.contains(firstCard))
    }

    @Test
    fun removeTop_whenEmpty_returnsNull() {
        // Arrange
        val cards = GameCardIDs(cardManager, emptyList(), randomizer)
        
        // Act
        val removed = cards.removeTop()
        
        // Assert
        assertNull(removed)
    }

    @Test
    fun clear_removesAllCards() {
        // Arrange
        val cards = GameCardIDs(cardManager, listOf(
            createTestCardId(1),
            createTestCardId(2),
            createTestCardId(3)
        ), randomizer)
        
        // Act
        cards.clear()
        
        // Assert
        assertTrue(cards.isEmpty())
        assertEquals(0, cards.size)
    }

    @Test
    fun take_whenRequestingLessThanAvailable_returnsSubset() {
        // Arrange
        val originalCards = listOf(
            createTestCardId(1),
            createTestCardId(2),
            createTestCardId(3)
        )
        val cards = GameCardIDs(cardManager, originalCards, randomizer)
        
        // Act
        val taken = cards.take(2)
        
        // Assert
        assertEquals(2, taken.size)
        assertEquals(originalCards.take(2), taken.cardIds)
        assertEquals(3, cards.size) // Original collection unchanged
    }

    @Test
    fun take_whenRequestingMoreThanAvailable_returnsAllCards() {
        // Arrange
        val originalCards = listOf(
            createTestCardId(1),
            createTestCardId(2)
        )
        val cards = GameCardIDs(cardManager, originalCards, randomizer)
        
        // Act
        val taken = cards.take(5)
        
        // Assert
        assertEquals(2, taken.size)
        assertEquals(originalCards, taken.cardIds)
        assertEquals(2, cards.size) // Original collection unchanged
    }

    @Test
    fun take_whenEmpty_returnsEmptyCollection() {
        // Arrange
        val cards = GameCardIDs(cardManager, emptyList(), randomizer)
        
        // Act
        val taken = cards.take(3)
        
        // Assert
        assertTrue(taken.isEmpty())
        assertEquals(0, taken.size)
    }

    @Test
    fun map_whenAllCardsExist_returnsTransformedList() {
        // Arrange
        val card1 = createTestCardId(1)
        val card2 = createTestCardId(2)
        val cards = GameCardIDs(cardManager, listOf(card1, card2), randomizer)
        
        val gameCard1 = mockk<GameCard>()
        val gameCard2 = mockk<GameCard>()
        every { cardManager.getCard(card1) } returns gameCard1
        every { cardManager.getCard(card2) } returns gameCard2
        every { gameCard1.name } returns "Card 1"
        every { gameCard2.name } returns "Card 2"
        
        // Act
        val result = cards.map { it.name }
        
        // Assert
        assertEquals(listOf("Card 1", "Card 2"), result)
    }

    @Test
    fun map_whenSomeCardsDontExist_skipsMissingCards() {
        // Arrange
        val card1 = createTestCardId(1)
        val card2 = createTestCardId(2)
        val cards = GameCardIDs(cardManager, listOf(card1, card2), randomizer)
        
        val gameCard1 = mockk<GameCard>()
        every { cardManager.getCard(card1) } returns gameCard1
        every { cardManager.getCard(card2) } returns null
        every { gameCard1.name } returns "Card 1"
        
        // Act
        val result = cards.map { it.name }
        
        // Assert
        assertEquals(listOf("Card 1"), result)
    }

    @Test
    fun map_whenNoCardsExist_returnsEmptyList() {
        // Arrange
        val card1 = createTestCardId(1)
        val card2 = createTestCardId(2)
        val SUT = GameCardIDs(cardManager, listOf(card1, card2), randomizer)
        
        every { cardManager.getCard(any<Int>()) } returns null
        
        // Act
        val result = SUT.map { it.name }
        
        // Assert
        assertTrue(result.isEmpty())
    }
} 
