package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.game.purchase.ManagePurchasedFloralTypes
import dugsolutions.leaf.market.Market
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectPossibleCardsTest {

    companion object {
        private const val PLAYER_ID = 1
        private const val PLAYER_NAME = "Test Player"
    }

    private lateinit var SUT: SelectPossibleCards

    private lateinit var market: Market
    private lateinit var managePurchasedFloralTypes: ManagePurchasedFloralTypes
    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        market = mockk(relaxed = true)
        managePurchasedFloralTypes = mockk(relaxed = true)
        
        // Create SelectPossibleCards instance
        SUT = SelectPossibleCards(market, managePurchasedFloralTypes)
        
        // Create mock player
        player = mockk(relaxed = true)
        every { player.id } returns PLAYER_ID
        every { player.name } returns PLAYER_NAME
    }

    @Test
    fun invoke_whenNoCardsPurchasedAndEnoughPips_returnsAllEligibleCards() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val bloomCard = FakeCards.fakeBloom
        val seedlingCard = FakeCards.fakeSeedling
        val topCards = listOf(rootCard, bloomCard, seedlingCard)
        
        every { market.getTopShowingCards() } returns topCards
        every { player.bloomCount } returns 0
        every { managePurchasedFloralTypes.has(any()) } returns false
        
        // Act
        val result = SUT(player)
        
        // Assert
        assertEquals(3, result.size)
        assertTrue(result.contains(rootCard))
        assertTrue(result.contains(bloomCard))
        assertTrue(result.contains(seedlingCard))
    }

    @Test
    fun invoke_whenTypeAlreadyPurchased_excludesThatType() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val bloomCard = FakeCards.fakeBloom
        val seedlingCard = FakeCards.fakeSeedling
        val topCards = listOf(rootCard, bloomCard, seedlingCard)
        
        every { market.getTopShowingCards() } returns topCards
        every { player.bloomCount } returns 0
        every { managePurchasedFloralTypes.has(FlourishType.ROOT) } returns true
        every { managePurchasedFloralTypes.has(FlourishType.BLOOM) } returns false
        every { managePurchasedFloralTypes.has(FlourishType.SEEDLING) } returns false
        
        // Act
        val result = SUT(player)
        
        // Assert
        assertEquals(2, result.size)
        assertTrue(result.contains(bloomCard))
        assertTrue(result.contains(seedlingCard))
    }

    @Test
    fun invoke_whenMaxBloomsReached_excludesBloomCards() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val bloomCard = FakeCards.fakeBloom
        val seedlingCard = FakeCards.fakeSeedling
        val topCards = listOf(rootCard, bloomCard, seedlingCard)
        
        every { market.getTopShowingCards() } returns topCards
        every { player.bloomCount } returns 2  // Max blooms reached
        every { managePurchasedFloralTypes.has(any()) } returns false
        
        // Act
        val result = SUT(player)
        
        // Assert
        assertEquals(2, result.size)
        assertTrue(result.contains(rootCard))
        assertTrue(result.contains(seedlingCard))
    }

    @Test
    fun invoke_whenMultipleRestrictions_appliesAllFilters() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val bloomCard = FakeCards.fakeBloom
        val seedlingCard = FakeCards.fakeSeedling
        val canopyCard = FakeCards.fakeCanopy
        val topCards = listOf(rootCard, bloomCard, seedlingCard, canopyCard)
        
        every { market.getTopShowingCards() } returns topCards
        every { player.bloomCount } returns 2  // Max blooms reached
        every { managePurchasedFloralTypes.has(FlourishType.ROOT) } returns false
        every { managePurchasedFloralTypes.has(FlourishType.BLOOM) } returns false
        every { managePurchasedFloralTypes.has(FlourishType.SEEDLING) } returns true
        every { managePurchasedFloralTypes.has(FlourishType.CANOPY) } returns false
        
        // Act
        val result = SUT(player)
        
        // Assert
        assertEquals(2, result.size)
        assertTrue(result.contains(rootCard))
        assertTrue(result.contains(canopyCard))
    }

    @Test
    fun invoke_whenNoEligibleCards_returnsEmptyList() {
        // Arrange
        val bloomCard = FakeCards.fakeBloom
        val seedlingCard = FakeCards.fakeSeedling
        val topCards = listOf(bloomCard, seedlingCard)
        
        every { market.getTopShowingCards() } returns topCards
        every { player.bloomCount } returns 2  // Max blooms reached
        every { managePurchasedFloralTypes.has(FlourishType.BLOOM) } returns false
        every { managePurchasedFloralTypes.has(FlourishType.SEEDLING) } returns true
        
        // Act
        val result = SUT(player)
        
        // Assert
        assertTrue(result.isEmpty())
    }
} 
