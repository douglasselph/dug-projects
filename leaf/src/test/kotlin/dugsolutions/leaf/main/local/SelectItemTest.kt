package dugsolutions.leaf.main.local

import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.CardInfoFaker
import dugsolutions.leaf.main.domain.DiceInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.PlayerInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SelectItemTest {

    companion object {
        private const val PLAYER_NAME = "Test Player"
        private const val CARD_NAME = "Test Card"
    }

    private lateinit var mockPlayerInfo: PlayerInfo
    private lateinit var mockCardInfo: CardInfo
    private lateinit var mockDieInfo: DieInfo

    private val SUT = SelectItem()

    @BeforeEach
    fun setup() {
        mockPlayerInfo = PlayerInfo(
            name = PLAYER_NAME,
            infoLine = PlayerScore(1, 0, 0).toString(),
            handCards = emptyList(),
            handDice = DiceInfo(emptyList()),
            supplyDice = DiceInfo(emptyList()),
            compostDice = DiceInfo(emptyList()),
            floralArray = emptyList(),
            supplyCardCount = 0,
            compostCardCount = 0,
            showDrawCount = false
        )
        mockCardInfo = CardInfoFaker.create().copy(
            name = CARD_NAME,
            index = 0,
            highlight = HighlightInfo.NONE
        )
        mockDieInfo = DieInfo(
            index = 0,
            value = "2D6",
            highlight = HighlightInfo.NONE
        )
    }

    @Test
    fun handCard_whenCardExists_updatesHighlight() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            handCards = listOf(mockCardInfo)
        )

        // Act
        val result = SUT.handCard(playerInfo, mockCardInfo)

        // Assert
        assertEquals(HighlightInfo.SELECTED, result.handCards[0].highlight)
    }

    @Test
    fun handCard_whenCardDoesNotExist_returnsOriginalPlayerInfo() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            handCards = emptyList()
        )

        // Act
        val result = SUT.handCard(playerInfo, mockCardInfo)

        // Assert
        assertSame(playerInfo, result)
    }

    @Test
    fun floralCard_whenCardExists_updatesHighlight() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            floralArray = listOf(mockCardInfo)
        )

        // Act
        val result = SUT.floralCard(playerInfo, mockCardInfo)

        // Assert
        assertEquals(HighlightInfo.SELECTED, result.floralArray[0].highlight)
    }

    @Test
    fun floralCard_whenCardDoesNotExist_returnsOriginalPlayerInfo() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            floralArray = emptyList()
        )

        // Act
        val result = SUT.floralCard(playerInfo, mockCardInfo)

        // Assert
        assertSame(playerInfo, result)
    }

    @Test
    fun die_whenDieExists_updatesHighlight() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            handDice = DiceInfo(listOf(mockDieInfo))
        )

        // Act
        val result = SUT.die(playerInfo, mockDieInfo)

        // Assert
        assertEquals(HighlightInfo.SELECTED, result.handDice.values[0].highlight)
    }

    @Test
    fun die_whenDieDoesNotExist_returnsOriginalPlayerInfo() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            handDice = DiceInfo(emptyList())
        )

        // Act
        val result = SUT.die(playerInfo, mockDieInfo)

        // Assert
        assertSame(playerInfo, result)
    }

    @Test
    fun die_whenIndexOutOfBounds_returnsOriginalPlayerInfo() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            handDice = DiceInfo(listOf(mockDieInfo))
        )
        val invalidDieInfo = mockDieInfo.copy(index = 1)

        // Act
        val result = SUT.die(playerInfo, invalidDieInfo)

        // Assert
        assertSame(playerInfo, result)
    }
} 
