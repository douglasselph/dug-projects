package dugsolutions.leaf.game.battle

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BattlePhaseTransitionTest {

    private lateinit var mockPlayer: Player
    private lateinit var mockBestFlowerCards: BestFlowerCards
    private lateinit var mockMatchingBloomCard: MatchingBloomCard

    private lateinit var SUT: BattlePhaseTransition

    @BeforeEach
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        mockBestFlowerCards = mockk(relaxed = true)
        mockMatchingBloomCard = mockk(relaxed = true)

        SUT = BattlePhaseTransition(mockBestFlowerCards, mockMatchingBloomCard)
    }

    @Test
    fun invoke_whenNoFlowerCards_doesNotAddAnyBlooms() {
        // Arrange
        every { mockBestFlowerCards(mockPlayer) } returns emptyList()
        every { mockPlayer.floralCards } returns emptyList()

        // Act
        SUT(listOf(mockPlayer))

        // Assert
        verify(exactly = 0) { mockPlayer.addCardToSupply(any()) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.resupply() }
    }

    @Test
    fun invoke_whenSingleTypeFlowers_addsTwoBloomsOfThatType() {
        // Arrange
        val flower1 = FakeCards.fakeFlower
        val bloom = FakeCards.fakeBloom

        every { mockBestFlowerCards(mockPlayer) } returns listOf(flower1)
        every { mockMatchingBloomCard(flower1) } returns bloom
        every { mockPlayer.floralCards } returns listOf(flower1)

        // Act
        SUT(listOf(mockPlayer))

        // Assert
        verify(exactly = 2) { mockPlayer.addCardToSupply(bloom.id) }
        verify { mockPlayer.addCardToSupply(flower1.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.resupply() }
    }

    @Test
    fun invoke_whenTwoFlowerTypes_addsTwoBlooms() {
        // Arrange
        val flower1 = FakeCards.fakeFlower
        val flower2 = FakeCards.fakeFlower2
        val bloom1 = FakeCards.fakeBloom
        val bloom2 = FakeCards.fakeBloom2

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
        verify { mockPlayer.resupply() }
    }

    @Test
    fun invoke_whenManyFlowerTypes_gathersBloomOfMostFlowers() {
        // Arrange
        val flower1 = FakeCards.fakeFlower
        val flower2 = FakeCards.fakeFlower2
        val flower3 = FakeCards.fakeFlower3
        val bloom1 = FakeCards.fakeBloom
        val bloom2 = FakeCards.fakeBloom2
        val bloom3 = FakeCards.fakeBloom3

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
        verify { mockPlayer.resupply() }
    }

    @Test
    fun invoke_whenManyFlowerTypes2_gathersBloomOfMostFlowers() {
        // Arrange
        val flower1 = FakeCards.fakeFlower
        val flower2 = FakeCards.fakeFlower2
        val flower3 = FakeCards.fakeFlower3
        val bloom1 = FakeCards.fakeBloom
        val bloom2 = FakeCards.fakeBloom2
        val bloom3 = FakeCards.fakeBloom3

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
        verify { mockPlayer.resupply() }
    }

    @Test
    fun invoke_whenSinglePlayerWithTwoFlowerCards_addsCardsToSupplyAndClearsFloralArray() {
        // Arrange
        val flower1 = FakeCards.fakeFlower
        val flower2 = FakeCards.fakeFlower2
        val bloom1 = FakeCards.fakeBloom
        val bloom2 = FakeCards.fakeBloom2

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
        verify { mockPlayer.resupply() }
    }
    
    @Test
    fun invoke_whenSinglePlayerWithOneFlowerCard_addsCardTwiceToSupply() {
        // Arrange
        val flower1 = FakeCards.fakeFlower
        val bloom1 = FakeCards.fakeBloom

        every { mockBestFlowerCards(mockPlayer) } returns listOf(flower1)
        every { mockMatchingBloomCard(flower1) } returns bloom1
        every { mockPlayer.floralCards } returns listOf(flower1)

        // Act
        SUT(listOf(mockPlayer))
        
        // Assert
        verify(exactly = 2) { mockPlayer.addCardToSupply(bloom1.id) }
        verify(exactly = 1) { mockPlayer.addCardToSupply(flower1.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockPlayer.resupply() }
    }
    
    @Test
    fun invoke_whenMultiplePlayers_processesEachPlayer() {
        // Arrange
        val mockPlayer2 = mockk<Player>(relaxed = true)
        val flower1 = FakeCards.fakeFlower
        val flower2 = FakeCards.fakeFlower2
        val bloom1 = FakeCards.fakeBloom
        val bloom2 = FakeCards.fakeBloom2

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
        verify { mockPlayer.resupply() }
        
        verify(exactly = 2) { mockPlayer2.addCardToSupply(bloom1.id) }
        verify { mockPlayer2.addCardToSupply(flower1.id) }
        verify { mockPlayer2.clearFloralCards() }
        verify { mockPlayer2.resupply() }
    }

} 
