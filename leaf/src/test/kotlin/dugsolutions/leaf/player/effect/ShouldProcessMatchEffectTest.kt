package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldProcessMatchEffectTest {

    private lateinit var mockHasDieValue: HasDieValue
    private lateinit var mockHasFlourishType: HasFlourishType
    private lateinit var mockCardManager: CardManager
    private lateinit var mockPlayer: Player
    private lateinit var mockCard: GameCard
    private lateinit var mockD4: Die
    private lateinit var mockD6: Die

    private lateinit var SUT: ShouldProcessMatchEffect

    @BeforeEach
    fun setup() {
        mockHasDieValue = mockk(relaxed = true)
        mockCardManager = mockk(relaxed = true)
        mockHasFlourishType = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockCard = mockk(relaxed = true)
        mockD4 = mockk(relaxed = true)
        mockD6 = mockk(relaxed = true)

        SUT = ShouldProcessMatchEffect(mockHasDieValue, mockHasFlourishType)
    }

    @Test
    fun invoke_whenMatchWithNone_returnsFalse() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.None

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenMatchWithOnRollAndHasMatchingDie_returnsTrue() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.OnRoll(6)
        every { mockPlayer.getItemsInHand() } returns listOf(HandItem.Dice(mockD6))
        every { mockHasDieValue(any(), 6) } returns true

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenMatchWithOnRollAndNoMatchingDie_returnsFalse() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.OnRoll(6)
        every { mockPlayer.getItemsInHand() } returns listOf(HandItem.Dice(mockD4))
        every { mockHasDieValue(any(), 6) } returns false

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenMatchWithOnFlourishTypeAndHasMatchingCard_returnsTrue() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.OnFlourishType(FlourishType.ROOT)
        every { mockPlayer.getItemsInHand() } returns listOf(HandItem.Card(mockCard))
        every { mockHasFlourishType(any(), FlourishType.ROOT) } returns true

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenMatchWithOnFlourishTypeAndNoMatchingCard_returnsFalse() {
        // Arrange
        every { mockCard.matchWith } returns MatchWith.OnFlourishType(FlourishType.ROOT)
        every { mockPlayer.getItemsInHand() } returns listOf(HandItem.Card(mockCard))
        every { mockHasFlourishType(any(), FlourishType.ROOT) } returns false

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertFalse(result)
    }
} 
