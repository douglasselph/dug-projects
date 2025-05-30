package dugsolutions.leaf.main

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.gather.GatherCardInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo
import dugsolutions.leaf.main.gather.GatherGroveInfo
import dugsolutions.leaf.main.gather.GatherPlayerInfo
import dugsolutions.leaf.main.gather.MainDomainManager
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainDomainManagerTest {

    companion object {
        private const val PLAYER_NAME_1 = "Player 1"
        private const val PLAYER_NAME_2 = "Player 2"
        private const val TURN = 5
    }
    private val mockGame = mockk<Game>(relaxed = true)
    private val gameTurn = GameTurn()
    private val gatherCardInfo = GatherCardInfo()
    private val gatherDiceInfo = GatherDiceInfo()
    private val gatherPlayerInfo = GatherPlayerInfo(gatherCardInfo, gatherDiceInfo)
    private val gatherGroveInfo = mockk<GatherGroveInfo>(relaxed = true)
    private val mockPlayer1 = mockk<Player>(relaxed = true)
    private val mockPlayer2 = mockk<Player>(relaxed = true)
    private val mockGroveInfo = mockk<GroveInfo>(relaxed = true)
    private val SUT = MainDomainManager(mockGame, gameTurn, gatherPlayerInfo, gatherGroveInfo)

    @BeforeEach
    fun setup() {
        every { mockGame.players } returns listOf(mockPlayer1, mockPlayer2)
        every { mockPlayer1.name } returns PLAYER_NAME_1
        every { mockPlayer2.name } returns PLAYER_NAME_2
        every { mockPlayer1.cardsInHand } returns emptyList()
        every { mockPlayer2.cardsInHand } returns emptyList()
        every { mockPlayer1.diceInHand } returns Dice(emptyList())
        every { mockPlayer2.diceInHand } returns Dice(emptyList())
        every { mockPlayer1.diceInSupply } returns Dice(emptyList())
        every { mockPlayer2.diceInSupply } returns Dice(emptyList())
        every { mockPlayer1.diceInCompost } returns Dice(emptyList())
        every { mockPlayer2.diceInCompost } returns Dice(emptyList())
        every { mockPlayer1.floralCards } returns emptyList()
        every { mockPlayer2.floralCards } returns emptyList()
        every { mockPlayer1.cardsInSupplyCount } returns 0
        every { mockPlayer2.cardsInSupplyCount } returns 0
        every { mockPlayer1.cardsInCompostCount } returns 0
        every { mockPlayer2.cardsInCompostCount } returns 0
        every { gatherGroveInfo() } returns mockGroveInfo
    }
    @Test
    fun update_whenGameHasPlayersAndGrove_updatesAllComponents() = runBlocking {
        // Arrange
        gameTurn.turn = TURN

        // Act
        SUT.update()

        // Assert
        val state = SUT.state.first()
        assertEquals(TURN, state.turn)
        assertEquals(2, state.players.size)
        assertEquals(PLAYER_NAME_1, state.players[0].name)
        assertEquals(PLAYER_NAME_2, state.players[1].name)
        assertEquals(mockGroveInfo, state.groveInfo)
    }

    @Test
    fun updateSimulationOutput_whenMultipleOutputs_appendsToExistingOutput() = runBlocking {
        // Arrange
        val initialOutput = "Initial output"
        val newOutput = "New output"
        val expectedResult = listOf(initialOutput, newOutput)

        // Act
        SUT.addSimulationOutput(initialOutput)
        SUT.addSimulationOutput(newOutput)

        // Assert
        val state = SUT.state.first()
        assertEquals(expectedResult, state.simulationOutput)
    }

    @Test
    fun showDrawCount_whenEnabled_updatesStepModeState() = runBlocking {
        // Arrange
        SUT.update()
        SUT.setShowDrawCount(mockPlayer1, false)

        // Act
        SUT.setShowDrawCount(mockPlayer1, true)

        // Assert
        val state = SUT.state.first()
        assertEquals(true, state.players.find { it.name == PLAYER_NAME_1}?.showDrawCount)
    }

    @Test
    fun setShowRunButton_whenShown_updatesRunButtonVisibility() = runBlocking {
        // Arrange
        SUT.setShowRunButton(false)
        SUT.setShowRunButton(false)

        // Act
        SUT.setShowRunButton(true)

        // Assert
        val state = SUT.state.first()
        assertEquals(true, state.showRunButton)
    }

    @Test
    fun setShowNextButton_whenShown_updatesNextButtonVisibility() = runBlocking {
        // Arrange
        SUT.setShowNextButton(false)
        SUT.setShowNextButton(false)

        // Act
        SUT.setShowNextButton(true)

        // Assert
        val state = SUT.state.first()
        assertEquals(true, state.showNextButton)
    }

    @Test
    fun clearShowDrawCount_whenEnabled_clearsDrawCountState() = runBlocking {
        // Arrange
        SUT.update()
        SUT.setShowDrawCount(mockPlayer1, true)
        var state = SUT.state.first()
        assertEquals(true, state.players.find { it.name == PLAYER_NAME_1 }?.showDrawCount)

        // Act
        SUT.clearShowDrawCount()

        // Assert
        state = SUT.state.first()
        assertEquals(false, state.players.find { it.name == PLAYER_NAME_1 }?.showDrawCount)
    }

    @Test
    fun clearShowRunButton_whenEnabled_clearsRunButtonState() = runBlocking {
        // Arrange
        SUT.setShowRunButton(true)
        var state = SUT.state.first()
        assertEquals(true, state.showRunButton)

        // Act
        SUT.clearShowRunButton()

        // Assert
        state = SUT.state.first()
        assertEquals(false, state.showRunButton)
    }

    @Test
    fun clearShowNextButton_whenEnabled_clearsNextButtonState() = runBlocking {
        // Arrange
        SUT.setShowNextButton(true)
        var state = SUT.state.first()
        assertEquals(true, state.showNextButton)

        // Act
        SUT.clearShowNextButton()

        // Assert
        state = SUT.state.first()
        assertEquals(false, state.showNextButton)
    }
} 
