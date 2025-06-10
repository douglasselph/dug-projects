package dugsolutions.leaf.main.gather

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.SelectedItems
import dugsolutions.leaf.main.local.SelectGather
import dugsolutions.leaf.main.local.SelectItem
import dugsolutions.leaf.player.PlayerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class MainDomainManagerTest {

    companion object {
        private const val TURN = 5
        private const val ACTION_INSTRUCTION = "Test Instruction"
    }

    private val mockGame = mockk<Game>(relaxed = true)
    private val gameTime = GameTime()
    private val gatherCardInfo = GatherCardInfo()
    private val gatherDiceInfo = GatherDiceInfo()
    private val gatherPlayerInfo = GatherPlayerInfo(gatherCardInfo, gatherDiceInfo)
    private val gatherGroveInfo = mockk<GatherGroveInfo>(relaxed = true)
    private val fakePlayer1 = PlayerTD(1)
    private val fakePlayer2 = PlayerTD(2)
    private val fakeFlower = FakeCards.fakeFlower
    private val fakeRoot = FakeCards.fakeRoot
    private val mockGroveInfo = mockk<GroveInfo>(relaxed = true)
    private val mockSelectItem = mockk<SelectItem>(relaxed = true)
    private val mockSelectGather = mockk<SelectGather>(relaxed = true)
    private val sampleDie = SampleDie()

    private val SUT = MainDomainManager(
        mockGame, gameTime, gatherPlayerInfo, gatherGroveInfo,
        mockSelectItem, mockSelectGather
    )

    @BeforeEach
    fun setup() {
        every { mockGame.players } returns listOf(fakePlayer1, fakePlayer2)
        every { gatherGroveInfo() } returns mockGroveInfo
        gameTime.turn = TURN
        fakePlayer1.addCardToHand(fakeRoot)
        fakePlayer1.addCardToFloralArray(fakeFlower)
        fakePlayer1.addDieToHand(sampleDie.d4)
        fakePlayer1.addDieToHand(sampleDie.d6)
        SUT.initialize()
    }

    @Test
    fun initialize_whenGameHasPlayersAndGrove_initializesAllComponents() = runBlocking {
        // Arrange
        // Act
        // Assert
        val state = SUT.state.first()
        assertEquals(TURN, state.turn)
        assertEquals(2, state.players.size)
        assertEquals(fakePlayer1.name, state.players[0].name)
        assertEquals(fakePlayer2.name, state.players[1].name)
        assertEquals(mockGroveInfo, state.groveInfo)
    }

    @Test
    fun setShowDrawCount_whenEnabled_updatesDrawCountState() = runBlocking {
        // Arrange
        SUT.setShowDrawCount(fakePlayer1, false)

        // Act
        SUT.setShowDrawCount(fakePlayer1, true)

        // Assert
        val state = SUT.state.first()
        assertTrue(state.players.find { it.name == fakePlayer1.name }?.showDrawCount ?: false)
    }

    @Test
    fun clearShowDrawCount_whenEnabled_clearsDrawCountState() = runBlocking {
        // Arrange
        SUT.setShowDrawCount(fakePlayer1, true)

        // Act
        SUT.clearShowDrawCount()

        // Assert
        val state = SUT.state.first()
        assertFalse(state.players.find { it.name == fakePlayer1.name }?.showDrawCount ?: true)
    }

    @Test
    fun setActionButton_whenSet_updatesActionButtonAndInstruction() = runBlocking {
        // Arrange
        val actionButton = ActionButton.RUN

        // Act
        SUT.setActionButton(actionButton, ACTION_INSTRUCTION)

        // Assert
        val state = SUT.state.first()
        assertEquals(actionButton, state.actionButton)
        assertEquals(ACTION_INSTRUCTION, state.actionInstruction)
    }

    @Test
    fun setStepMode_whenEnabled_updatesStepModeState() = runBlocking {
        // Arrange
        // Act
        SUT.setStepMode(true)

        // Assert
        val state = SUT.state.first()
        assertTrue(state.stepModeEnabled)
    }

    @Test
    fun setHighlightGroveItemsForSelection_whenCalledWithCardsAndDice_updatesGroveInfo() = runBlocking {
        // Arrange
        val possibleCards = listOf(fakeRoot)
        val possibleDice = listOf(sampleDie.d6)
        val updatedGroveInfo = mockk<GroveInfo>()
        every { gatherGroveInfo(possibleCards, possibleDice, fakePlayer1) } returns updatedGroveInfo

        // Act
        SUT.setHighlightGroveItemsForSelection(possibleCards, possibleDice, fakePlayer1)

        // Assert
        val state = SUT.state.first()
        assertEquals(updatedGroveInfo, state.groveInfo)
        verify { gatherGroveInfo(possibleCards, possibleDice, fakePlayer1) }
    }

    @Test
    fun setHighlightGroveItemsForSelection_whenCalledWithOnlyCards_updatesGroveInfo() = runBlocking {
        // Arrange
        val possibleCards = listOf(fakeRoot)
        val possibleDice = emptyList<Die>()
        val updatedGroveInfo = mockk<GroveInfo>()
        every { gatherGroveInfo(possibleCards, possibleDice, fakePlayer1) } returns updatedGroveInfo

        // Act
        SUT.setHighlightGroveItemsForSelection(possibleCards, possibleDice, fakePlayer1)

        // Assert
        val state = SUT.state.first()
        assertEquals(updatedGroveInfo, state.groveInfo)
        verify { gatherGroveInfo(possibleCards, possibleDice, fakePlayer1) }
    }

    @Test
    fun setHighlightGroveItemsForSelection_whenCalledWithOnlyDice_updatesGroveInfo() = runBlocking {
        // Arrange
        val possibleCards = emptyList<GameCard>()
        val possibleDice = listOf(sampleDie.d6)
        val updatedGroveInfo = mockk<GroveInfo>()
        every { gatherGroveInfo(possibleCards, possibleDice, fakePlayer1) } returns updatedGroveInfo

        // Act
        SUT.setHighlightGroveItemsForSelection(possibleCards, possibleDice, fakePlayer1)

        // Assert
        val state = SUT.state.first()
        assertEquals(updatedGroveInfo, state.groveInfo)
        verify { gatherGroveInfo(possibleCards, possibleDice, fakePlayer1) }
    }

    @Test
    fun clearGroveCardHighlights_whenCalled_updatesGroveInfo() = runBlocking {
        // Arrange
        // Act
        SUT.clearGroveCardHighlights()

        // Assert
        val state = SUT.state.first()
        assertEquals(mockGroveInfo, state.groveInfo)
        verify { gatherGroveInfo() }
    }

    @Test
    fun setAllowPlayerItemSelect_whenCalled_updatesPlayerInfo() = runBlocking {
        // Arrange
        val playerInfo1 = gatherPlayerInfo(fakePlayer1)
        val modifiedPlayerInfo = playerInfo1.copyForItemSelect()

        // Act
        SUT.setAllowPlayerItemSelect(fakePlayer1)

        // Assert
        val state = SUT.state.first()
        val updatedPlayer = state.players.find { it.name == playerInfo1.name }
        assertEquals(modifiedPlayerInfo, updatedPlayer)
    }

    @Test
    fun clearPlayerItemSelect_whenCalled_updatesAllPlayerInfo() = runBlocking {
        // Arrange
        val playerInfo1 = gatherPlayerInfo(fakePlayer1)
        val playerInfo2 = gatherPlayerInfo(fakePlayer2)

        // Act
        SUT.clearPlayerSelect()

        // Assert
        val state = SUT.state.first()
        assertEquals(2, state.players.size)
        assertEquals(playerInfo1, state.players[0])
        assertEquals(playerInfo2, state.players[1])
    }

    @Test
    fun setHandCardSelected_whenCalled_updatesPlayerInfo() = runBlocking {
        // Arrange
        val playerInfo1 = gatherPlayerInfo(fakePlayer1)
        val cardInfo = playerInfo1.handCards.find { it.name == fakeRoot.name }
        assertNotNull(cardInfo)
        val updatedPlayerInfo = playerInfo1.copy(
            handCards = playerInfo1.handCards.map {
                if (it.index == cardInfo.index) it.copy(highlight = HighlightInfo.SELECTED) else it
            }
        )
        every { mockSelectItem.handCard(playerInfo1, cardInfo) } returns updatedPlayerInfo

        // Act
        SUT.setHandCardSelected(playerInfo1, cardInfo)

        // Assert
        val state = SUT.state.first()
        val updatedPlayer = state.players.find { it.name == playerInfo1.name }
        assertTrue(updatedPlayer?.handCards?.any { it.index == cardInfo.index && it.highlight == HighlightInfo.SELECTED } ?: false)
    }

    @Test
    fun setFloralCardSelected_whenCalled_updatesPlayerInfo() = runBlocking {
        // Arrange
        val playerInfo1 = gatherPlayerInfo(fakePlayer1)
        val cardInfo = playerInfo1.floralArray.find { it.name == fakeFlower.name }
        assertNotNull(cardInfo)
        val updatedPlayerInfo = playerInfo1.copy(
            floralArray = playerInfo1.floralArray.map {
                if (it.index == cardInfo.index) it.copy(highlight = HighlightInfo.SELECTED) else it
            }
        )
        every { mockSelectItem.floralCard(playerInfo1, cardInfo) } returns updatedPlayerInfo

        // Act
        SUT.setFloralCardSelected(playerInfo1, cardInfo)

        // Assert
        val state = SUT.state.first()
        val updatedPlayer = state.players.find { it.name == fakePlayer1.name }
        assertNotNull(updatedPlayer)
        assertTrue(updatedPlayer.floralArray.any { it.index == cardInfo.index && it.highlight == HighlightInfo.SELECTED })
    }

    @Test
    fun setDieSelected_whenCalled_updatesPlayerInfo() = runBlocking {
        // Arrange
        val playerInfo1 = gatherPlayerInfo(fakePlayer1)
        val dieInfo = playerInfo1.handDice.values[0]
        val updatedPlayerInfo = playerInfo1.copy(
            handDice = playerInfo1.handDice.copy(
                values = playerInfo1.handDice.values.map {
                    if (it.index == dieInfo.index) it.copy(highlight = HighlightInfo.SELECTED) else it
                }
            )
        )
        every { mockSelectItem.die(playerInfo1, dieInfo) } returns updatedPlayerInfo

        // Act
        SUT.setDieSelected(playerInfo1, dieInfo)

        // Assert
        val state = SUT.state.first()
        val updatedPlayer = state.players.find { it.name == playerInfo1.name }
        assertTrue(updatedPlayer?.handDice?.values?.any { it.index == dieInfo.index && it.highlight == HighlightInfo.SELECTED } ?: false)
    }

    @Test
    fun gatherSelected_whenCalled_returnsSelectedItems() = runBlocking {
        // Arrange
        val state = SUT.state.first()
        val mockSelectedItems = mockk<SelectedItems>(relaxed = true)
        every { mockSelectGather(state) } returns mockSelectedItems
        val mockGameCards = mockk<List<GameCard>>(relaxed = true)
        every { mockSelectedItems.cards } returns mockGameCards
        val mockFloralCards = mockk<List<GameCard>>(relaxed = true)
        every { mockSelectedItems.floralCards } returns mockFloralCards
        val mockDice = mockk<List<Die>>(relaxed = true)
        every { mockSelectedItems.dice } returns mockDice

        // Act
        val result = SUT.gatherSelected()

        // Assert
        assertEquals(mockGameCards, result.cards)
        assertEquals(mockFloralCards, result.floralCards)
        assertEquals(mockDice, result.dice)
        verify { mockSelectGather(state) }
    }

    @Test
    fun addSimulationOutput_whenCalled_appendsToOutput() = runBlocking {
        // Arrange
        val initialOutput = "Initial output"
        val newOutput = "New output"

        // Act
        SUT.addSimulationOutput(initialOutput)
        SUT.addSimulationOutput(newOutput)

        // Assert
        val state = SUT.state.first()
        assertEquals(listOf(initialOutput, newOutput), state.simulationOutput)
    }

    @Test
    fun setAskTrash_whenSetToTrue_updatesAskTrashEnabledToTrue() = runBlocking {
        // Arrange
        // Act
        SUT.setAskTrash(true)

        // Assert
        val state = SUT.state.first()
        assertTrue(state.askTrashEnabled)
    }

    @Test
    fun setAskTrash_whenSetToFalse_updatesAskTrashEnabledToFalse() = runBlocking {
        // Arrange
        SUT.setAskTrash(true) // Set to true first

        // Act
        SUT.setAskTrash(false)

        // Assert
        val state = SUT.state.first()
        assertFalse(state.askTrashEnabled)
    }

    @Test
    fun setAskTrash_whenCalledMultipleTimes_updatesStateCorrectly() = runBlocking {
        // Arrange
        // Act & Assert - First call
        SUT.setAskTrash(true)
        var state = SUT.state.first()
        assertTrue(state.askTrashEnabled)

        // Act & Assert - Second call
        SUT.setAskTrash(false)
        state = SUT.state.first()
        assertFalse(state.askTrashEnabled)

        // Act & Assert - Third call
        SUT.setAskTrash(true)
        state = SUT.state.first()
        assertTrue(state.askTrashEnabled)
    }
} 
