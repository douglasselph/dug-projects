package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecisionDamageAbsorptionBaselineTest {

    private val mockPlayer = mockk<Player>(relaxed = true)
    private val mockCardManager = mockk<CardManager>(relaxed = true)
    private val SUT: DecisionDamageAbsorptionBaseline = DecisionDamageAbsorptionBaseline(mockPlayer, mockCardManager)

    companion object {
        private const val NO_DAMAGE = 0
        private const val LIGHT_DAMAGE = 2
        private const val MEDIUM_DAMAGE = 4
        private const val HEAVY_DAMAGE = 7
        private const val OVERWHELMING_DAMAGE = 20
    }

    @BeforeEach
    fun setUp() {
        // Reset player state
        every { mockPlayer.incomingDamage } returns 0
        every { mockPlayer.cardsInHand() } returns emptyList()
        every { mockPlayer.creatureLeafCards } returns emptyList()
    }

    @Test
    fun invoke_whenNoDamage_returnsEmptyResult() = runBlocking {
        // Arrange
        every { mockPlayer.incomingDamage } returns NO_DAMAGE

        // Act
        val result = SUT()

        // Assert
        assertEquals(DecisionDamageAbsorption.Result(), result)
    }

    @Test
    fun invoke_whenNegativeDamage_returnsEmptyResult() = runBlocking {
        // Arrange
        every { mockPlayer.incomingDamage } returns -1

        // Act
        val result = SUT()

        // Assert
        assertEquals(DecisionDamageAbsorption.Result(), result)
    }

    @Test
    fun invoke_whenHandCardsAvailable_selectsOptimalHandCards() = runBlocking {
        // Arrange
        val handCards = listOf(FakeCards.weakRootCard, FakeCards.strongRootCard, FakeCards.massiveRootCard)
        every { mockPlayer.incomingDamage } returns MEDIUM_DAMAGE
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns emptyList()

        // Act
        val result = SUT()

        // Assert
        assertEquals(1, result.handCards.size)
        assertEquals(FakeCards.massiveRootCard, result.handCards[0])
        assertEquals(FakeCards.massiveRootCard.resilience, result.damageAbsorbed)
        assertEquals(0, result.damageStillLeftToAbsorb)
        assertTrue(result.creatureCards.isEmpty())
    }

    @Test
    fun invoke_whenExactMatchAvailable_selectsExactMatch() = runBlocking {
        // Arrange
        val handCards = listOf(FakeCards.weakRootCard, FakeCards.strongRootCard, FakeCards.massiveRootCard)
        every { mockPlayer.incomingDamage } returns 3 // Exact match with strongRootCard
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns emptyList()

        // Act
        val result = SUT()

        // Assert
        assertEquals(1, result.handCards.size)
        assertEquals(FakeCards.strongRootCard, result.handCards[0])
        assertEquals(3, result.damageAbsorbed)
        assertEquals(0, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenNoExactMatch_selectsClosestMatch() = runBlocking {
        // Arrange
        val handCards = listOf(FakeCards.weakRootCard, FakeCards.strongRootCard, FakeCards.massiveRootCard)
        assertEquals(1, FakeCards.weakRootCard.resilience)
        assertEquals(3, FakeCards.strongRootCard.resilience)
        assertEquals(5, FakeCards.massiveRootCard.resilience)
        every { mockPlayer.incomingDamage } returns 2 // No exact match, closest is strongRootCard (3)
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns emptyList()

        // Act
        val result = SUT()

        // Assert
        assertEquals(1, result.handCards.size)
        assertEquals(FakeCards.strongRootCard, result.handCards[0])
        assertEquals(3, result.damageAbsorbed)
        assertEquals(0, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenMultipleCardsNeeded_selectsOptimalCombination() = runBlocking {
        // Arrange
        val handCards = listOf(FakeCards.weakRootCard, FakeCards.weakRootCard, FakeCards.strongRootCard)
        every { mockPlayer.incomingDamage } returns 3 // Need 3 total, should select strongRootCard (3) over two weakRootCards (1+1=2)
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns emptyList()

        // Act
        val result = SUT()

        // Assert
        assertEquals(1, result.handCards.size)
        assertEquals(FakeCards.strongRootCard, result.handCards[0])
        assertEquals(3, result.damageAbsorbed)
        assertEquals(0, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenHandCardsInsufficient_usesCreatureCards() = runBlocking {
        // Arrange
        assertEquals(1, FakeCards.weakRootCard.resilience)
        val handCards = listOf(FakeCards.weakRootCard) // Only 1 resilience
        val creatureCards = listOf(FakeCards.strongVineCard, FakeCards.strongFlowerCard)
        assertEquals(4, FakeCards.strongVineCard.resilience)
        assertEquals(3, FakeCards.strongFlowerCard.resilience)
        assertEquals(7, creatureCards.sumOf { it.resilience})
        every { mockPlayer.incomingDamage } returns HEAVY_DAMAGE // 7 damage
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns creatureCards

        // Act
        val result = SUT()

        // Assert
        assertEquals(1, result.handCards.size)
        assertEquals(FakeCards.weakRootCard, result.handCards[0])
        assertEquals(2, result.creatureCards.size)
        assertTrue(result.creatureCards.contains(FakeCards.strongVineCard))
        assertTrue(result.creatureCards.contains(FakeCards.strongFlowerCard))
        assertEquals(8, result.damageAbsorbed) // 1 + 4
        assertEquals(0, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenNoHandCards_usesCreatureCards() = runBlocking {
        // Arrange
        val creatureCards = listOf(FakeCards.weakVineCard, FakeCards.strongFlowerCard)
        assertEquals(2, FakeCards.weakVineCard.resilience)
        assertEquals(3,  FakeCards.strongFlowerCard.resilience)
        every { mockPlayer.incomingDamage } returns MEDIUM_DAMAGE
        every { mockPlayer.cardsInHand() } returns emptyList()
        every { mockPlayer.creatureLeafCards } returns creatureCards

        // Act
        val result = SUT()

        // Assert
        assertTrue(result.handCards.isEmpty())
        assertEquals(2, result.creatureCards.size)
        assertTrue(result.creatureCards.contains(FakeCards.weakVineCard))
        assertTrue(result.creatureCards.contains(FakeCards.strongFlowerCard))
        assertEquals(5, result.damageAbsorbed)
        assertEquals(0, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenOverwhelmingDamage_usesAllCards() = runBlocking {
        // Arrange
        val handCards = listOf(FakeCards.weakRootCard, FakeCards.strongRootCard)
        val creatureCards = listOf(FakeCards.weakVineCard, FakeCards.strongFlowerCard)
        every { mockPlayer.incomingDamage } returns OVERWHELMING_DAMAGE // 20 damage
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns creatureCards

        // Act
        val result = SUT()

        // Assert
        assertEquals(2, result.handCards.size)
        assertEquals(2, result.creatureCards.size)
        assertEquals(9, result.damageAbsorbed) // 1 + 3 + 2 + 3 = 9
        assertEquals(11, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenNoCardsAvailable_returnsEmptyResult() = runBlocking {
        // Arrange
        every { mockPlayer.incomingDamage } returns MEDIUM_DAMAGE
        every { mockPlayer.cardsInHand() } returns emptyList()
        every { mockPlayer.creatureLeafCards } returns emptyList()

        // Act
        val result = SUT()

        // Assert
        assertTrue(result.handCards.isEmpty())
        assertTrue(result.creatureCards.isEmpty())
        assertEquals(0, result.damageAbsorbed)
        assertEquals(MEDIUM_DAMAGE, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenHandCardsHaveZeroResilience_skipsToCreatureCards() = runBlocking {
        // Arrange
        val handCards = listOf(FakeCards.sunlightCard, FakeCards.waterCard) // RESOURCE cards have 0 resilience
        val creatureCards = listOf(FakeCards.strongFlowerCard)
        every { mockPlayer.incomingDamage } returns LIGHT_DAMAGE
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns creatureCards
        assertEquals(3, FakeCards.strongFlowerCard.resilience)

        // Act
        val result = SUT()

        // Assert
        assertEquals(2, result.handCards.size)
        assertEquals(1, result.creatureCards.size)
        assertEquals(FakeCards.strongFlowerCard, result.creatureCards[0])
        assertEquals(3, result.damageAbsorbed)
        assertEquals(0, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenCreatureCardsNotMuch_handlesGracefully() = runBlocking {
        // Arrange
        val handCards = listOf(FakeCards.strongRootCard)
        assertEquals(3, FakeCards.strongFlowerCard.resilience)
        val creatureCards = listOf(FakeCards.weakRootCard, FakeCards.weakFlowerCard)
        assertEquals(1, FakeCards.weakRootCard.resilience)
        assertEquals(1, FakeCards.weakFlowerCard.resilience)
        every { mockPlayer.incomingDamage } returns HEAVY_DAMAGE
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns creatureCards

        // Act
        val result = SUT()

        // Assert
        assertEquals(1, result.handCards.size)
        assertEquals(FakeCards.strongRootCard, result.handCards[0])
        assertEquals(2, result.creatureCards.size)
        assertEquals(5, result.damageAbsorbed)
        assertEquals(2, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenMultipleOptimalCombinations_selectsFewestCards() = runBlocking {
        // Arrange
        val handCards = listOf(
            FakeCards.weakRootCard, // 1
            FakeCards.weakRootCard, // 1
            FakeCards.weakRootCard, // 1
            FakeCards.strongRootCard // 3
        )
        every { mockPlayer.incomingDamage } returns 3
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns emptyList()

        // Act
        val result = SUT()

        // Assert
        assertEquals(1, result.handCards.size)
        assertEquals(FakeCards.strongRootCard, result.handCards[0])
        assertEquals(3, result.damageAbsorbed)
        assertEquals(0, result.damageStillLeftToAbsorb)
    }

    @Test
    fun invoke_whenExactCombinationAvailable_selectsExactCombination() = runBlocking {
        // Arrange
        val handCards = listOf(
            FakeCards.weakRootCard, // 1
            FakeCards.strongRootCard, // 3
            FakeCards.massiveRootCard // 5
        )
        every { mockPlayer.incomingDamage } returns 4 // Should select weakRootCard + strongRootCard = 4
        every { mockPlayer.cardsInHand() } returns handCards
        every { mockPlayer.creatureLeafCards } returns emptyList()

        // Act
        val result = SUT()

        // Assert
        assertEquals(1, result.handCards.size)
        assertTrue(result.handCards.contains(FakeCards.massiveRootCard))
        assertEquals(FakeCards.massiveRootCard.resilience, result.damageAbsorbed)
        assertEquals(0, result.damageStillLeftToAbsorb)
    }
}
