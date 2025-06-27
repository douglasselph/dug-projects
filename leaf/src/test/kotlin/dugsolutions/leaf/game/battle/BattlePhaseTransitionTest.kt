package dugsolutions.leaf.game.battle

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.game.turn.handle.HandleDrawHand
import dugsolutions.leaf.player.Player
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BattlePhaseTransitionTest {

    private val mockPlayer: Player = mockk(relaxed = true)
    private lateinit var mockBestFlowerCards: BestFlowerCards
    private lateinit var mockMatchingBloomCard: MatchingBloomCard
    private lateinit var mockChronicle: GameChronicle
    private val mockHandleDrawHand: HandleDrawHand = mockk(relaxed = true)

    private lateinit var SUT: BattlePhaseTransition

    @BeforeEach
    fun setup() {
        mockBestFlowerCards = mockk(relaxed = true)
        mockMatchingBloomCard = mockk(relaxed = true)
        mockChronicle = mockk(relaxed = true)

        SUT = BattlePhaseTransition(mockBestFlowerCards, mockMatchingBloomCard, mockHandleDrawHand, mockChronicle)
    }

    @Test
    fun invoke_whenNoFlowerCards_doesNotAddAnyBlooms() = runBlocking {
        // Arrange
        every { mockBestFlowerCards(mockPlayer) } returns emptyList()
        every { mockPlayer.floralCards } returns emptyList()

        // Act
        SUT(listOf(mockPlayer))

        // Assert
        verify(exactly = 0) { mockPlayer.addCardToSupply(any()) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.reset() }
        coVerify { mockHandleDrawHand(mockPlayer) }
    }

    @Test
    fun invoke_whenSingleTypeFlowers_addsTwoBloomsOfThatType() = runBlocking {
        // Arrange
        val flower1 = FakeCards.flowerCard
        val bloom = FakeCards.bloomCard

        every { mockBestFlowerCards(mockPlayer) } returns listOf(flower1)
        every { mockMatchingBloomCard(flower1) } returns bloom
        every { mockPlayer.floralCards } returns listOf(flower1)

        // Act
        SUT(listOf(mockPlayer))

        // Assert
        verify(exactly = 2) { mockPlayer.addCardToSupply(bloom.id) }
        verify { mockPlayer.addCardToSupply(flower1.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.reset() }
        verify { mockPlayer.trashSeedlingCards() }
        verify { mockChronicle(any()) }
        coVerify { mockHandleDrawHand(mockPlayer) }
    }

    @Test
    fun invoke_whenTwoFlowerTypes_addsTwoBlooms() = runBlocking {
        // Arrange
        val flower1 = FakeCards.flowerCard
        val flower2 = FakeCards.flowerCard2
        val bloom1 = FakeCards.bloomCard
        val bloom2 = FakeCards.bloomCard2

        every { mockBestFlowerCards(mockPlayer) } returns listOf(flower1, flower2)
        every { mockMatchingBloomCard(flower1) } returns bloom1
        every { mockMatchingBloomCard(flower2) } returns bloom2
        every { mockPlayer.floralCards } returns listOf(flower1, flower2)

        // Act
        SUT(listOf(mockPlayer))

        // Assert
        verify(exactly = 1) { mockPlayer.addCardToSupply(bloom1.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(bloom2.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(flower1.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(flower2.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.trashSeedlingCards() }
        verify { mockPlayer.reset() }
        coVerify { mockHandleDrawHand(mockPlayer) }
    }

    @Test
    fun invoke_whenManyFlowerTypes_gathersBloomOfMostFlowers() = runBlocking {
        // Arrange
        val flower1 = FakeCards.flowerCard
        val flower2 = FakeCards.flowerCard2
        val flower3 = FakeCards.flowerCard3
        val bloom1 = FakeCards.bloomCard
        val bloom2 = FakeCards.bloomCard2
        val bloom3 = FakeCards.bloomCard3

        assertEquals(flower1.id, bloom1.flowerCardId)
        assertEquals(flower2.id, bloom2.flowerCardId)
        assertEquals(flower3.id, bloom3.flowerCardId)

        every { mockBestFlowerCards(mockPlayer) } returns listOf(flower1, flower3)
        every { mockMatchingBloomCard(flower1) } returns bloom1
        every { mockMatchingBloomCard(flower3) } returns bloom3
        every { mockPlayer.floralCards } returns listOf(flower1, flower1, flower2, flower3, flower3)

        // Act
        SUT(listOf(mockPlayer))

        // Assert
        verify(exactly = 1) { mockPlayer.addCardToSupply(bloom1.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(bloom3.id) }
        verify(exactly = 2) { mockPlayer.addCardToSupply(flower1.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(flower2.id) }
        verify(exactly = 2) { mockPlayer.addCardToSupply(flower3.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.trashSeedlingCards() }
        verify { mockPlayer.reset() }
        coVerify { mockHandleDrawHand(mockPlayer) }
    }

    @Test
    fun invoke_whenManyFlowerTypes2_gathersBloomOfMostFlowers() = runBlocking {
        // Arrange
        val flower1 = FakeCards.flowerCard
        val flower2 = FakeCards.flowerCard2
        val flower3 = FakeCards.flowerCard3
        val bloom1 = FakeCards.bloomCard
        val bloom2 = FakeCards.bloomCard2
        val bloom3 = FakeCards.bloomCard3

        assertEquals(flower1.id, bloom1.flowerCardId)
        assertEquals(flower2.id, bloom2.flowerCardId)
        assertEquals(flower3.id, bloom3.flowerCardId)

        every { mockBestFlowerCards(mockPlayer) } returns listOf(flower1, flower2)
        every { mockMatchingBloomCard(flower1) } returns bloom1
        every { mockMatchingBloomCard(flower2) } returns bloom2
        every { mockPlayer.floralCards } returns listOf(flower1, flower1, flower2, flower2, flower3, flower3)

        // Act
        SUT(listOf(mockPlayer))

        // Assert
        verify(exactly = 1) { mockPlayer.addCardToSupply(bloom1.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(bloom2.id) }
        verify(exactly = 2) { mockPlayer.addCardToSupply(flower1.id) }
        verify(exactly = 2) { mockPlayer.addCardToSupply(flower2.id) }
        verify(exactly = 2) { mockPlayer.addCardToSupply(flower3.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.trashSeedlingCards() }
        verify { mockPlayer.reset() }
        coVerify { mockHandleDrawHand(mockPlayer) }
    }

    @Test
    fun invoke_whenSinglePlayerWithTwoFlowerCards_addsCardsToSupplyAndClearsFloralArray() = runBlocking {
        // Arrange
        val flower1 = FakeCards.flowerCard
        val flower2 = FakeCards.flowerCard2
        val bloom1 = FakeCards.bloomCard
        val bloom2 = FakeCards.bloomCard2

        every { mockBestFlowerCards(mockPlayer) } returns listOf(flower1, flower2)
        every { mockMatchingBloomCard(flower1) } returns bloom1
        every { mockMatchingBloomCard(flower2) } returns bloom2
        every { mockPlayer.floralCards } returns listOf(flower1, flower2)

        // Act
        SUT(listOf(mockPlayer))

        // Assert
        verify(exactly = 1) { mockPlayer.addCardToSupply(flower1.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(flower2.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(bloom1.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(bloom2.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.trashSeedlingCards() }
        verify { mockPlayer.reset() }
        coVerify { mockHandleDrawHand(mockPlayer) }
    }

    @Test
    fun invoke_whenSinglePlayerWithOneFlowerCard_addsCardTwiceToSupply() = runBlocking {
        // Arrange
        val flower1 = FakeCards.flowerCard
        val bloom1 = FakeCards.bloomCard

        every { mockBestFlowerCards(mockPlayer) } returns listOf(flower1)
        every { mockMatchingBloomCard(flower1) } returns bloom1
        every { mockPlayer.floralCards } returns listOf(flower1)

        // Act
        SUT(listOf(mockPlayer))

        // Assert
        verify(exactly = 2) { mockPlayer.addCardToSupply(bloom1.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(flower1.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.trashSeedlingCards() }
        verify { mockPlayer.reset() }
        coVerify { mockHandleDrawHand(mockPlayer) }
    }

    @Test
    fun invoke_whenMultiplePlayers_processesEachPlayer() = runBlocking {
        // Arrange
        val mockPlayer2 = mockk<Player>(relaxed = true)
        val flower1 = FakeCards.flowerCard
        val flower2 = FakeCards.flowerCard2
        val bloom1 = FakeCards.bloomCard
        val bloom2 = FakeCards.bloomCard2

        every { mockBestFlowerCards(mockPlayer) } returns listOf(flower1, flower2)
        every { mockBestFlowerCards(mockPlayer2) } returns listOf(flower1)
        every { mockMatchingBloomCard(flower1) } returns bloom1
        every { mockMatchingBloomCard(flower2) } returns bloom2
        every { mockPlayer.floralCards } returns listOf(flower1, flower2)
        every { mockPlayer2.floralCards } returns listOf(flower1)

        // Act
        SUT(listOf(mockPlayer, mockPlayer2))

        // Assert
        verify { mockPlayer.addCardToSupply(flower1.id) }
        verify { mockPlayer.addCardToSupply(flower2.id) }
        verify { mockPlayer.addCardToSupply(bloom1.id) }
        verify { mockPlayer.addCardToSupply(bloom2.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.reset() }

        verify(exactly = 2) { mockPlayer2.addCardToSupply(bloom1.id) }
        verify { mockPlayer2.addCardToSupply(flower1.id) }
        verify { mockPlayer2.clearFloralCards() }
        verify { mockPlayer2.reset() }
        verify { mockPlayer.trashSeedlingCards() }
        coVerify { mockHandleDrawHand(mockPlayer) }
    }

} 
