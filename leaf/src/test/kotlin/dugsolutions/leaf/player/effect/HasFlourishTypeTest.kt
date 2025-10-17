package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.domain.HandItem
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasFlourishTypeTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
    }

    private lateinit var mockCardManager: CardManager
    private lateinit var mockCard: HandItem.aCard
    private lateinit var mockADie: HandItem.aDie
    private lateinit var mockDie: Die
    private lateinit var mockGameCard: GameCard

    private lateinit var SUT: HasFlourishType

    @BeforeEach
    fun setup() {
        mockCardManager = mockk(relaxed = true)
        mockDie = mockk(relaxed = true)
        mockADie = HandItem.aDie(mockDie)
        mockGameCard = mockk(relaxed = true) {
            every { id } returns CARD_ID_1
            every { type } returns FlourishType.ROOT
        }
        mockCard = HandItem.aCard(mockGameCard)
        SUT = HasFlourishType(mockCardManager)

        every { mockCardManager.getCard(CARD_ID_1) } returns mockGameCard
    }

    @Test
    fun invoke_whenEmptyList_returnsFalse() {
        // Act
        val result = SUT(emptyList(), FlourishType.ROOT)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenOnlyDice_returnsFalse() {
        // Act
        val result = SUT(listOf(mockADie), FlourishType.ROOT)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenCardWithMatchingType_returnsTrue() {
        // Act
        val result = SUT(listOf(mockCard), FlourishType.ROOT)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenCardWithDifferentType_returnsFalse() {
        // Act
        val result = SUT(listOf(mockCard), FlourishType.BLOOM)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenMixedItemsWithMatchingCard_returnsTrue() {
        // Act
        val result = SUT(listOf(mockCard, mockADie), FlourishType.ROOT)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenCardNotFound_returnsFalse() {
        // Arrange
        val unknownCard = HandItem.aCard(mockGameCard)
        every { mockCard.card.id } returns CARD_ID_2
        every { mockCardManager.getCard(CARD_ID_2) } returns null

        // Act
        val result = SUT(listOf(unknownCard), FlourishType.ROOT)

        // Assert
        assertFalse(result)
    }
} 
