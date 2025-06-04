package dugsolutions.leaf.main.local

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.CardInfoFaker
import dugsolutions.leaf.main.domain.DiceInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.DieInfoFaker
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.domain.PlayerInfoFaker
import dugsolutions.leaf.main.domain.SelectedItems
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SelectGatherTest {

    companion object {
        private const val PLAYER_NAME = "Test Player"
    }

    private val mockCardManager = mockk<CardManager>(relaxed = true)
    private val mockMainDomain = mockk<MainDomain>(relaxed = true)
    private val mockGameCard = mockk<GameCard>(relaxed = true)
    private val mockDie = mockk<Die>(relaxed = true)

    private val SUT = SelectGather(mockCardManager)

    @BeforeEach
    fun setup() {
        every { mockMainDomain.players } returns listOf(PlayerInfoFaker.create(name = PLAYER_NAME))
        every { mockCardManager.getCard(any<String>()) } returns mockGameCard
    }

    @Test
    fun invoke_whenNoSelectedItems_returnsEmptySelectedItems() {
        // Arrange
        val playerInfo = PlayerInfoFaker.createEmpty(name = PLAYER_NAME)
        every { mockMainDomain.players } returns listOf(playerInfo)

        // Act
        val result = SUT(mockMainDomain)

        // Assert
        assertTrue(result.cards.isEmpty())
        assertTrue(result.floralCards.isEmpty())
        assertTrue(result.dice.isEmpty())
    }

    @Test
    fun invoke_whenHandCardSelected_returnsSelectedCard() {
        // Arrange
        val cardInfo = CardInfoFaker.createSelected()
        val playerInfo = PlayerInfoFaker.create(
            name = PLAYER_NAME,
            handCardCount = 1
        ).copy(handCards = listOf(cardInfo))
        every { mockMainDomain.players } returns listOf(playerInfo)

        // Act
        val result = SUT(mockMainDomain)

        // Assert
        assertEquals(listOf(mockGameCard), result.cards)
        assertTrue(result.floralCards.isEmpty())
        assertTrue(result.dice.isEmpty())
        verify { mockCardManager.getCard(cardInfo.name) }
    }

    @Test
    fun invoke_whenFloralCardSelected_returnsSelectedCard() {
        // Arrange
        val cardInfo = CardInfoFaker.createSelected()
        val playerInfo = PlayerInfoFaker.create(
            name = PLAYER_NAME,
            floralCardCount = 1
        ).copy(floralArray = listOf(cardInfo))
        every { mockMainDomain.players } returns listOf(playerInfo)

        // Act
        val result = SUT(mockMainDomain)

        // Assert
        assertTrue(result.cards.isEmpty())
        assertEquals(listOf(mockGameCard), result.floralCards)
        assertTrue(result.dice.isEmpty())
        verify { mockCardManager.getCard(cardInfo.name) }
    }

    @Test
    fun invoke_whenDieSelected_returnsSelectedDie() {
        // Arrange
        val dieInfo = DieInfoFaker.createSelected()
        val playerInfo = PlayerInfoFaker.create(
            name = PLAYER_NAME,
            handDieCount = 1
        ).copy(handDice = DiceInfo(listOf(dieInfo.copy(backingDie = mockDie))))
        every { mockMainDomain.players } returns listOf(playerInfo)

        // Act
        val result = SUT(mockMainDomain)

        // Assert
        assertTrue(result.cards.isEmpty())
        assertTrue(result.floralCards.isEmpty())
        assertEquals(listOf(mockDie), result.dice)
    }

    @Test
    fun invoke_whenMultipleItemsSelected_returnsAllSelectedItems() {
        // Arrange
        val handCard = CardInfoFaker.createSelected()
        val floralCard = CardInfoFaker.createSelected()
        val dieInfo = DieInfoFaker.createSelected()
        val playerInfo = PlayerInfoFaker.create(
            name = PLAYER_NAME,
            handCardCount = 1,
            handDieCount = 1,
            floralCardCount = 1
        ).copy(
            handCards = listOf(handCard),
            floralArray = listOf(floralCard),
            handDice = DiceInfo(listOf(dieInfo.copy(backingDie = mockDie)))
        )
        every { mockMainDomain.players } returns listOf(playerInfo)

        // Act
        val result = SUT(mockMainDomain)

        // Assert
        assertEquals(listOf(mockGameCard), result.cards)
        assertEquals(listOf(mockGameCard), result.floralCards)
        assertEquals(listOf(mockDie), result.dice)
        verify { mockCardManager.getCard(handCard.name) }
        verify { mockCardManager.getCard(floralCard.name) }
    }
} 
