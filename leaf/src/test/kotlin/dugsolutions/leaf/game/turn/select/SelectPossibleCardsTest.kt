package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.game.acquire.ManageAcquiredFloralTypes
import dugsolutions.leaf.grove.Grove
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectPossibleCardsTest {

    private lateinit var grove: Grove
    private lateinit var manageAcquiredFloralTypes: ManageAcquiredFloralTypes

    private lateinit var SUT: SelectPossibleCards

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        grove = mockk(relaxed = true)
        manageAcquiredFloralTypes = mockk(relaxed = true)
        
        // Create SelectPossibleCards instance
        SUT = SelectPossibleCards(grove, manageAcquiredFloralTypes)
    }

    @Test
    fun invoke_whenNoCardsPurchasedAndEnoughPips_returnsAllEligibleCards() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val flowerCard = FakeCards.fakeFlower
        val vineCard = FakeCards.fakeVine
        val topCards = listOf(rootCard, flowerCard, vineCard)
        
        every { grove.getTopShowingCards() } returns topCards
        every { manageAcquiredFloralTypes.has(any()) } returns false
        
        // Act
        val result = SUT()
        
        // Assert
        assertEquals(3, result.size)
        assertTrue(result.contains(rootCard))
        assertTrue(result.contains(flowerCard))
        assertTrue(result.contains(vineCard))
    }

    @Test
    fun invoke_whenTypeAlreadyPurchased_excludesThatType() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val flowerCard = FakeCards.fakeFlower
        val vineCard = FakeCards.fakeVine
        val topCards = listOf(rootCard, flowerCard, vineCard)
        
        every { grove.getTopShowingCards() } returns topCards
        every { manageAcquiredFloralTypes.has(FlourishType.ROOT) } returns true
        every { manageAcquiredFloralTypes.has(FlourishType.FLOWER) } returns false
        every { manageAcquiredFloralTypes.has(FlourishType.VINE) } returns false
        
        // Act
        val result = SUT()
        
        // Assert
        assertEquals(2, result.size)
        assertTrue(result.contains(flowerCard))
        assertTrue(result.contains(vineCard))
    }

    @Test
    fun invoke_whenMaxBloomsReached_excludesBloomCards() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val bloomCard = FakeCards.fakeBloom
        val vineCard = FakeCards.fakeVine
        val topCards = listOf(rootCard, bloomCard, vineCard)
        
        every { grove.getTopShowingCards() } returns topCards
        every { manageAcquiredFloralTypes.has(any()) } returns false
        
        // Act
        val result = SUT()
        
        // Assert
        assertEquals(2, result.size)
        assertTrue(result.contains(rootCard))
        assertTrue(result.contains(vineCard))
    }

    @Test
    fun invoke_withSeedlingsAndBlooms_ignores() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val bloomCard = FakeCards.fakeBloom
        val seedlingCard = FakeCards.fakeSeedling
        val canopyCard = FakeCards.fakeCanopy
        val topCards = listOf(rootCard, bloomCard, seedlingCard, canopyCard)

        every { grove.getTopShowingCards() } returns topCards
        every { manageAcquiredFloralTypes.has(any()) } returns false

        // Act
        val result = SUT()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.contains(rootCard))
        assertTrue(result.contains(canopyCard))
    }

    @Test
    fun invoke_whenMultipleRestrictions_appliesAllFilters() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val bloomCard = FakeCards.fakeBloom
        val seedlingCard = FakeCards.fakeSeedling
        val canopyCard = FakeCards.fakeCanopy
        val vineCard = FakeCards.fakeVine
        val flowerCard = FakeCards.fakeFlower2
        val topCards = listOf(rootCard, bloomCard, seedlingCard, canopyCard, flowerCard, vineCard)
        
        every { grove.getTopShowingCards() } returns topCards
        every { manageAcquiredFloralTypes.has(any()) } returns false
        every { manageAcquiredFloralTypes.has(FlourishType.CANOPY) } returns true

        // Act
        val result = SUT()
        
        // Assert
        assertEquals(3, result.size)
        assertTrue(result.contains(rootCard))
        assertTrue(result.contains(vineCard))
        assertTrue(result.contains(flowerCard))
    }

    @Test
    fun invoke_whenNoEligibleCards_returnsEmptyList() {
        // Arrange
        val bloomCard = FakeCards.fakeBloom
        val seedlingCard = FakeCards.fakeSeedling
        val topCards = listOf(bloomCard, seedlingCard)
        
        every { grove.getTopShowingCards() } returns topCards
        every { manageAcquiredFloralTypes.has(FlourishType.FLOWER) } returns false
        every { manageAcquiredFloralTypes.has(FlourishType.SEEDLING) } returns true
        
        // Act
        val result = SUT()
        
        // Assert
        assertTrue(result.isEmpty())
    }
} 
