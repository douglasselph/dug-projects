package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CardsEffectsProcessorTest {

    private lateinit var mockCardEffectsProcessor: CardEffectsProcessor
    private lateinit var mockPlayer: Player

    private lateinit var SUT: CardsEffectsProcessor

    @BeforeEach
    fun setup() {
        mockCardEffectsProcessor = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)

        SUT = CardsEffectsProcessor(mockCardEffectsProcessor)
    }

    @Test
    fun invoke_whenEmptyList_doesNotProcessAnyCards() {
        // Arrange
        val emptyList = emptyList<GameCard>()

        // Act
        SUT(emptyList, mockPlayer)

        // Assert
        verify(exactly = 0) { mockCardEffectsProcessor(any(), any()) }
    }

    @Test
    fun invoke_whenSingleCard_processesThatCard() {
        // Arrange
        val card = FakeCards.fakeFlower

        // Act
        SUT(listOf(card), mockPlayer)

        // Assert
        verify(exactly = 1) { mockCardEffectsProcessor(card, mockPlayer) }
    }

    @Test
    fun invoke_whenMultipleCards_processesAllCards() {
        // Arrange
        val card1 = FakeCards.fakeFlower
        val card2 = FakeCards.fakeFlower2
        val card3 = FakeCards.fakeBloom

        // Act
        SUT(listOf(card1, card2, card3), mockPlayer)

        // Assert
        verify(exactly = 1) { mockCardEffectsProcessor(card1, mockPlayer) }
        verify(exactly = 1) { mockCardEffectsProcessor(card2, mockPlayer) }
        verify(exactly = 1) { mockCardEffectsProcessor(card3, mockPlayer) }
    }

    @Test
    fun invoke_whenDuplicateCards_processesEachCardOnce() {
        // Arrange
        val card = FakeCards.fakeFlower

        // Act
        SUT(listOf(card, card), mockPlayer)

        // Assert
        verify(exactly = 2) { mockCardEffectsProcessor(card, mockPlayer) }
    }
} 
