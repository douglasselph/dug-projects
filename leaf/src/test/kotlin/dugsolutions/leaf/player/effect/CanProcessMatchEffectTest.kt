package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CanProcessMatchEffectTest {

    private lateinit var mockHasDieValue: HasDieValue
    private lateinit var mockHasFlourishType: HasFlourishType
    private lateinit var mockCardManager: CardManager
    private lateinit var mockPlayer: Player
    private lateinit var mockCard: GameCard
    private lateinit var mockD4: Die
    private lateinit var mockD6: Die

    private lateinit var SUT: CanProcessMatchEffect

    @BeforeEach
    fun setup() {
        mockHasDieValue = mockk(relaxed = true)
        mockCardManager = mockk(relaxed = true)
        mockHasFlourishType = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockCard = mockk(relaxed = true)
        mockD4 = mockk(relaxed = true)
        mockD6 = mockk(relaxed = true)

        SUT = CanProcessMatchEffect(mockHasDieValue, mockHasFlourishType)
    }

    @Test
    fun invoke_whenMatchWithNone_returnsFalse() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.None
        val expectedResult = CanProcessMatchEffect.Result(false)

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenMatchWithOnRollAndHasMatchingDie_returnsTrue() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.OnRoll(6)
        every { mockPlayer.getItemsInHand() } returns listOf(HandItem.aDie(mockD6))
        every { mockHasDieValue(any(), 6) } returns mockD6
        val expectedResult = CanProcessMatchEffect.Result(true, mockD6)

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenMatchWithOnRollAndNoMatchingDie_returnsFalse() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.OnRoll(6)
        every { mockPlayer.getItemsInHand() } returns listOf(HandItem.aDie(mockD4))
        every { mockHasDieValue(any(), 6) } returns null
        val expectedResult = CanProcessMatchEffect.Result(false)

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenMatchWithOnFlourishTypeAndHasMatchingCard_returnsTrue() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.OnFlourishType(FlourishType.ROOT)
        every { mockPlayer.getItemsInHand() } returns listOf(HandItem.aCard(mockCard))
        every { mockHasFlourishType(any(), FlourishType.ROOT) } returns true
        val expectedResult = CanProcessMatchEffect.Result(true)

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenMatchWithOnFlourishTypeAndNoMatchingCard_returnsFalse() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.OnFlourishType(FlourishType.ROOT)
        every { mockPlayer.getItemsInHand() } returns listOf(HandItem.aCard(mockCard))
        every { mockHasFlourishType(any(), FlourishType.ROOT) } returns false
        val expectedResult = CanProcessMatchEffect.Result(false)

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(expectedResult, result)
    }
} 
