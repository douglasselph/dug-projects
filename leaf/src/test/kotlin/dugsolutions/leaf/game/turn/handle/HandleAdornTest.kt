package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.PlayerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HandleAdornTest {

    companion object {
        private const val FLOWER_CARD_1_ID = 1
        private const val FLOWER_CARD_2_ID = 2
        private const val ROOT_CARD_ID = 3
        private const val DRAWN_CARD_ID = 4
    }

    private val fakePlayer = PlayerTD.create2(1)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val mockCardManager: CardManager = mockk(relaxed = true)
    private val mockFlowerCard1: GameCard = mockk {
        every { id } returns FLOWER_CARD_1_ID
        every { type } returns FlourishType.FLOWER
    }
    private val mockFlowerCard2: GameCard = mockk {
        every { id } returns FLOWER_CARD_2_ID
        every { type } returns FlourishType.FLOWER
    }
    private val mockRootCard: GameCard = mockk {
        every { id } returns ROOT_CARD_ID
        every { type } returns FlourishType.ROOT
    }
    private val mockDrawnFlowerCard: GameCard = mockk {
        every { id } returns DRAWN_CARD_ID
        every { type } returns FlourishType.FLOWER
    }

    private val SUT = HandleAdorn(mockCardManager, mockChronicle)

    @BeforeEach
    fun setup() {
        fakePlayer.useDeckManager = false
        fakePlayer.cardsToPlay.clear()
        every { mockCardManager.getCard(FLOWER_CARD_1_ID) } returns mockFlowerCard1
        every { mockCardManager.getCard(FLOWER_CARD_2_ID) } returns mockFlowerCard2
        every { mockCardManager.getCard(ROOT_CARD_ID) } returns mockRootCard
        every { mockCardManager.getCard(DRAWN_CARD_ID) } returns mockDrawnFlowerCard
    }

    @Test
    fun invoke_whenCardsToPlayEmpty_doesNothing() {
        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(0, fakePlayer.gotFloralArrayCards.size)
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenNoFlowerCards_doesNothing() {
        // Arrange
        fakePlayer.cardsToPlay.add(mockRootCard)

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(0, fakePlayer.gotFloralArrayCards.size)
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_withSingleFlowerCard_processesCard() {
        // Arrange
        fakePlayer.cardsToPlay.add(mockFlowerCard1)

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(1, fakePlayer.gotFloralArrayCards.size)
        assertEquals(FLOWER_CARD_1_ID, fakePlayer.gotFloralArrayCards[0])
        assertFalse(fakePlayer.cardsToPlay.contains(mockFlowerCard1))
        verify { mockChronicle(Moment.ADORN(fakePlayer, flowerCardId = FLOWER_CARD_1_ID, drawCardId = 0)) }
    }

    @Test
    fun invoke_whenDrawnCardIsFlower_processesRecursively() {
        // Arrange
        fakePlayer.cardsToPlay.add(mockFlowerCard1)
        fakePlayer.addCardToSupply(mockDrawnFlowerCard)

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(2, fakePlayer.gotFloralArrayCards.size)
        assertTrue(fakePlayer.gotFloralArrayCards.contains(FLOWER_CARD_1_ID))
        assertTrue(fakePlayer.gotFloralArrayCards.contains(DRAWN_CARD_ID))
        assertFalse(fakePlayer.cardsToPlay.contains(mockFlowerCard1))
        verify { mockCardManager.getCard(DRAWN_CARD_ID) }
        verify { mockChronicle(Moment.ADORN(fakePlayer, flowerCardId = FLOWER_CARD_1_ID, drawCardId = DRAWN_CARD_ID)) }
        verify { mockChronicle(Moment.DRAW_CARD(fakePlayer, DRAWN_CARD_ID)) }
        verify { mockChronicle(Moment.ADORN(fakePlayer, flowerCardId = DRAWN_CARD_ID, drawCardId = 0)) }
    }

    @Test
    fun invoke_whenDrawnCardIsNotFlower_doesNotProcessFurther() {
        // Arrange
        fakePlayer.cardsToPlay.add(mockFlowerCard1)
        fakePlayer.addCardToSupply(mockRootCard)

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(1, fakePlayer.gotFloralArrayCards.size)
        assertEquals(FLOWER_CARD_1_ID, fakePlayer.gotFloralArrayCards[0])
        verify { mockCardManager.getCard(ROOT_CARD_ID) }
        verify { mockChronicle(Moment.ADORN(fakePlayer, flowerCardId = FLOWER_CARD_1_ID, drawCardId = ROOT_CARD_ID)) }
        verify { mockChronicle(Moment.DRAW_CARD(fakePlayer, ROOT_CARD_ID)) }
    }

    @Test
    fun invoke_withMultipleFlowerCards_processesAllCards() {
        // Arrange
        fakePlayer.cardsToPlay.add(mockFlowerCard1)
        fakePlayer.cardsToPlay.add(mockFlowerCard2)

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(2, fakePlayer.gotFloralArrayCards.size)
        assertTrue(fakePlayer.gotFloralArrayCards.contains(FLOWER_CARD_1_ID))
        assertTrue(fakePlayer.gotFloralArrayCards.contains(FLOWER_CARD_2_ID))
        verify { mockChronicle(Moment.ADORN(fakePlayer, flowerCardId = FLOWER_CARD_1_ID, drawCardId = 0)) }
        verify { mockChronicle(Moment.ADORN(fakePlayer, flowerCardId = FLOWER_CARD_2_ID, drawCardId = 0)) }
    }

    @Test
    fun invoke_whenNoCardDrawn_doesNotProcessFurther() {
        // Arrange
        fakePlayer.cardsToPlay.add(mockFlowerCard1)
        // No cards in supply, so drawCard will return 0

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(1, fakePlayer.gotFloralArrayCards.size)
        assertEquals(FLOWER_CARD_1_ID, fakePlayer.gotFloralArrayCards[0])
        verify { mockChronicle(Moment.ADORN(fakePlayer, flowerCardId = FLOWER_CARD_1_ID, drawCardId = 0)) }
        verify(exactly = 1) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenCardManagerReturnsNull_doesNotProcessFurther() {
        // Arrange
        fakePlayer.cardsToPlay.add(mockFlowerCard1)
        fakePlayer.addCardToSupply(mockDrawnFlowerCard)
        every { mockCardManager.getCard(DRAWN_CARD_ID) } returns null

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(1, fakePlayer.gotFloralArrayCards.size)
        assertEquals(FLOWER_CARD_1_ID, fakePlayer.gotFloralArrayCards[0])
        verify { mockCardManager.getCard(DRAWN_CARD_ID) }
        verify { mockChronicle(Moment.ADORN(fakePlayer, flowerCardId = FLOWER_CARD_1_ID, drawCardId = DRAWN_CARD_ID)) }
        verify { mockChronicle(Moment.DRAW_CARD(fakePlayer, DRAWN_CARD_ID)) }
    }
} 
