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
    private lateinit var fakeCardInfo: CardInfo
    private lateinit var fakeDieInfo: DieInfo

    private val SUT = SelectItem()

    @BeforeEach
    fun setup() {
        mockPlayerInfo = PlayerInfo(
            name = PLAYER_NAME,
            infoLine = PlayerScore(1, 0, 0).toString(),
            handCards = emptyList(),
            handDice = DiceInfo(emptyList()),
            supplyDice = DiceInfo(emptyList()),
            bedDice = DiceInfo(emptyList()),
            floralArray = emptyList(),
            nutrients = 0,
            supplyCardCount = 0,
            bedCardCount = 0
        )
        fakeCardInfo = CardInfoFaker.create().copy(
            name = CARD_NAME,
            index = 0,
            highlight = HighlightInfo.NONE
        )
        fakeDieInfo = DieInfo(
            index = 0,
            value = "2D6",
            highlight = HighlightInfo.NONE
        )
    }

    @Test
    fun handCard_whenCardExistsAndSelectable_updatesHighlight() {
        // Arrange
        val useCard = fakeCardInfo.copy(highlight = HighlightInfo.SELECTABLE)
        val playerInfo = mockPlayerInfo.copy(
            handCards = listOf(useCard)
        )

        // Act
        val result = SUT.handCard(playerInfo, useCard)

        // Assert
        assertEquals(HighlightInfo.SELECTED, result.handCards[0].highlight)
    }

    @Test
    fun handCard_whenCardExistsYetNotSelectable_doesNothing() {
        // Arrange
        val useCard = fakeCardInfo
        val playerInfo = mockPlayerInfo.copy(
            handCards = listOf(useCard)
        )

        // Act
        val result = SUT.handCard(playerInfo, useCard)

        // Assert
        assertEquals(HighlightInfo.NONE, result.handCards[0].highlight)
    }

    @Test
    fun handCard_whenCardDoesNotExist_returnsOriginalPlayerInfo() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(handCards = emptyList())

        // Act
        val result = SUT.handCard(playerInfo, fakeCardInfo)

        // Assert
        assertSame(playerInfo, result)
    }

    @Test
    fun floralCard_whenCardExists_selectable__updatesHighlight() {
        // Arrange
        fakeCardInfo = fakeCardInfo.copy(highlight = HighlightInfo.SELECTABLE)
        val playerInfo = mockPlayerInfo.copy(floralArray = listOf(fakeCardInfo))

        // Act
        val result = SUT.floralCard(playerInfo, fakeCardInfo)

        // Assert
        assertEquals(HighlightInfo.SELECTED, result.floralArray[0].highlight)
    }

    @Test
    fun floralCard_whenCardExists_selected__updatesHighlight() {
        // Arrange
        fakeCardInfo = fakeCardInfo.copy(highlight = HighlightInfo.SELECTED)
        val playerInfo = mockPlayerInfo.copy(floralArray = listOf(fakeCardInfo))

        // Act
        val result = SUT.floralCard(playerInfo, fakeCardInfo)

        // Assert
        assertEquals(HighlightInfo.SELECTABLE, result.floralArray[0].highlight)
    }

    @Test
    fun floralCard_whenCardExists_notSelectable__doesNothing() {
        // Arrange
        fakeCardInfo = fakeCardInfo.copy(highlight = HighlightInfo.NONE)
        val playerInfo = mockPlayerInfo.copy(floralArray = listOf(fakeCardInfo))

        // Act
        val result = SUT.floralCard(playerInfo, fakeCardInfo)

        // Assert
        assertEquals(HighlightInfo.NONE, result.floralArray[0].highlight)
    }

    @Test
    fun floralCard_whenCardDoesNotExist_returnsOriginalPlayerInfo() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            floralArray = emptyList()
        )

        // Act
        val result = SUT.floralCard(playerInfo, fakeCardInfo)

        // Assert
        assertSame(playerInfo, result)
    }

    @Test
    fun die_whenDieExists_selectable__updatesHighlight() {
        // Arrange
        fakeDieInfo = fakeDieInfo.copy(highlight = HighlightInfo.SELECTABLE)
        val playerInfo = mockPlayerInfo.copy(handDice = DiceInfo(listOf(fakeDieInfo)))

        // Act
        val result = SUT.die(playerInfo, fakeDieInfo)

        // Assert
        assertEquals(HighlightInfo.SELECTED, result.handDice.values[0].highlight)
    }

    @Test
    fun die_whenDieExists_selected__updatesHighlight() {
        // Arrange
        fakeDieInfo = fakeDieInfo.copy(highlight = HighlightInfo.SELECTED)
        val playerInfo = mockPlayerInfo.copy(handDice = DiceInfo(listOf(fakeDieInfo)))

        // Act
        val result = SUT.die(playerInfo, fakeDieInfo)

        // Assert
        assertEquals(HighlightInfo.SELECTABLE, result.handDice.values[0].highlight)
    }

    @Test
    fun die_whenDieExists_notSelectable__doesNothing() {
        // Arrange
        fakeDieInfo = fakeDieInfo.copy(highlight = HighlightInfo.NONE)
        val playerInfo = mockPlayerInfo.copy(handDice = DiceInfo(listOf(fakeDieInfo)))

        // Act
        val result = SUT.die(playerInfo, fakeDieInfo)

        // Assert
        assertEquals(HighlightInfo.NONE, result.handDice.values[0].highlight)
    }

    @Test
    fun die_whenDieDoesNotExist_returnsOriginalPlayerInfo() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            handDice = DiceInfo(emptyList())
        )

        // Act
        val result = SUT.die(playerInfo, fakeDieInfo)

        // Assert
        assertSame(playerInfo, result)
    }

    @Test
    fun die_whenIndexOutOfBounds_returnsOriginalPlayerInfo() {
        // Arrange
        val playerInfo = mockPlayerInfo.copy(
            handDice = DiceInfo(listOf(fakeDieInfo))
        )
        val invalidDieInfo = fakeDieInfo.copy(index = 1)

        // Act
        val result = SUT.die(playerInfo, invalidDieInfo)

        // Assert
        assertSame(playerInfo, result)
    }
} 
