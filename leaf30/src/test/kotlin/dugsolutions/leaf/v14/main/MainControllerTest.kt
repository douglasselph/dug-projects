package dugsolutions.leaf.v14.main

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.report.WriteGameResults
import dugsolutions.leaf.v14.game.Game
import dugsolutions.leaf.v14.game.RunGame
import dugsolutions.leaf.v14.grove.Grove
import dugsolutions.leaf.v14.grove.scenario.ScenarioBasicConfig
import dugsolutions.leaf.v14.main.gather.MainActionManager
import dugsolutions.leaf.v14.main.gather.MainGameManager
import dugsolutions.leaf.v14.main.gather.MainOutputManager
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.effect.NutrientReward
import dugsolutions.leaf.v14.random.RandomizerDefault
import io.mockk.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import dugsolutions.leaf.v14.main.domain.CardInfoFaker
import dugsolutions.leaf.v14.main.domain.PlayerInfoFaker
import dugsolutions.leaf.v14.main.domain.DieInfoFaker
import dugsolutions.leaf.v14.main.local.CardOperations
import dugsolutions.leaf.v14.main.local.MainActionHandler
import dugsolutions.leaf.v14.main.local.MainDecisions
import dugsolutions.leaf.v14.main.domain.ActionButton
import dugsolutions.leaf.v14.main.domain.ItemInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle

@OptIn(ExperimentalCoroutinesApi::class)
class MainControllerTest {

    private val mockGame: Game = mockk(relaxed = true)
    private val mockGrove: Grove = mockk(relaxed = true)
    private val mockScenarioBasicConfig: ScenarioBasicConfig = mockk(relaxed = true)
    private val mockCardOperations: CardOperations = mockk(relaxed = true)
    private val mockRandomizer: RandomizerDefault = mockk(relaxed = true)
    private val mockRunGame: RunGame = mockk(relaxed = true)
    private val mockMainGameManager: MainGameManager = mockk(relaxed = true)
    private val mockMainOutputManager: MainOutputManager = mockk(relaxed = true)
    private val mockMainDecisions: MainDecisions = mockk(relaxed = true)
    private val mockMainActionManager: MainActionManager = mockk(relaxed = true)
    private val mockMainActionHandler: MainActionHandler = mockk(relaxed = true)
    private val mockWriteGameResults: WriteGameResults = mockk(relaxed = true)
    private val mockNutrientReward: NutrientReward = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val SUT = MainController(
        game = mockGame,
        grove = mockGrove,
        scenarioBasicConfig = mockScenarioBasicConfig,
        cardOperations = mockCardOperations,
        randomizer = mockRandomizer,
        runGame = mockRunGame,
        mainGameManager = mockMainGameManager,
        mainOutputManager = mockMainOutputManager,
        mainDecisions = mockMainDecisions,
        mainActionManager = mockMainActionManager,
        mainActionHandler = mockMainActionHandler,
        writeGameResults = mockWriteGameResults,
        nutrientReward = mockNutrientReward,
        chronicle = mockChronicle,
        dispatcher = testDispatcher
    )

    private val fakePlayerInfo = PlayerInfoFaker.create(name = "TestPlayer")
    private val fakeCardInfo = CardInfoFaker.create(index = 0)
    private val fakeDieInfo = DieInfoFaker.create(index = 0)
    private val itemCard = ItemInfo.Card(fakeCardInfo)
    private val itemDie = ItemInfo.Die(fakeDieInfo)
    private val player = mockk<Player>(relaxed = true)

    @BeforeEach
    fun setup() {
        every { mockGame.players } returns listOf(player)
        every { player.name } returns fakePlayerInfo.name
    }

    @Test
    fun onActionPressed_whenRun_callsOnRunPressed() {
        // Arrange
        // Act
        SUT.onActionPressed(ActionButton.RUN)
        // Assert
        verify { mockMainActionHandler.clearAction() }
    }

    @Test
    fun onActionPressed_whenNext_callsOnNextButtonPressed() = runTest(testDispatcher) {
        // Arrange
        // Act
        SUT.onActionPressed(ActionButton.NEXT)
        advanceUntilIdle()
        // Assert
        coVerify { mockMainActionHandler.clearAction() }
    }

    @Test
    fun onActionPressed_whenDone_callsOnDoneButtonPressed() = runTest(testDispatcher) {
        // Act
        SUT.onActionPressed(ActionButton.DONE)
        advanceUntilIdle()
        // Assert
        verify { mockMainDecisions.onPlayerSelectionComplete() }
        verify { mockMainActionHandler.clearAction() }
    }

    @Test
    fun onStepEnabledToggled_whenCalled_setsStepMode() {
        // Arrange
        // Act
        SUT.onStepEnabledToggled(true)
        // Assert
        verify { mockRunGame.stepMode = true }
        verify { mockMainGameManager.setStepMode(true) }
    }

    @Test
    fun onAskTrashToggled_whenCalled_delegatesToMainDecisions() {
        // Arrange
        // Act
        SUT.onAskTrashToggled(true)
        // Assert
        verify { mockMainDecisions.setAskTrash(true) }
    }

    @Test
    fun onDrawCountChosen_whenPlayerExists_delegatesToMainDecisions() {
        // Arrange
        every { mockGame.players } returns listOf(player)
        every { player.name } returns fakePlayerInfo.name
        // Act
        SUT.onDrawCountChosen(fakePlayerInfo, 2)
        // Assert
        verify { mockMainDecisions.onDrawCountChosen(player, 2) }
    }

    @Test
    fun onGroveItemSelected_whenCard_callsOnGroveCardSelected() {
        // Arrange
        // Act
        SUT.onGroveItemSelected(itemCard)
        // Assert
        verify { mockMainDecisions.onGroveCardSelected(fakeCardInfo) }
    }

    @Test
    fun onGroveItemSelected_whenDie_callsOnGroveDieSelected() {
        // Arrange
        // Act
        SUT.onGroveItemSelected(itemDie)
        // Assert
        verify { mockMainDecisions.onGroveDieSelected(fakeDieInfo) }
    }

    @Test
    fun onHandCardSelected_whenCalled_delegatesToMainGameManager() {
        // Arrange
        // Act
        SUT.onHandCardSelected(fakePlayerInfo, fakeCardInfo)
        // Assert
        verify { mockMainGameManager.setHandCardSelected(fakePlayerInfo, fakeCardInfo) }
    }

    @Test
    fun onFloralCardSelected_whenCalled_delegatesToMainGameManager() {
        // Arrange
        // Act
        SUT.onFloralCardSelected(fakePlayerInfo, fakeCardInfo)
        // Assert
        verify { mockMainGameManager.setFloralCardSelected(fakePlayerInfo, fakeCardInfo) }
    }

    @Test
    fun onDieSelected_whenCalled_delegatesToMainGameManager() {
        // Arrange
        // Act
        SUT.onDieSelected(fakePlayerInfo, fakeDieInfo)
        // Assert
        verify { mockMainGameManager.setDieSelected(fakePlayerInfo, fakeDieInfo) }
    }

    @Test
    fun onNutrientsClicked_whenPlayerExists_callsNutrientRewardAndResetsData() {
        // Arrange
        every { mockGame.players } returns listOf(player)
        every { player.name } returns fakePlayerInfo.name
        // Act
        SUT.onNutrientsClicked(fakePlayerInfo)
        // Assert
        verify { mockNutrientReward.invoke(player) }
        verify { mockMainGameManager.resetData() }
        verify { mockMainDecisions.reapplyDecisionId() }
    }

    @Test
    fun onBooleanInstructionResponse_whenCalled_delegatesToMainDecisions() = runTest(testDispatcher) {
        // Arrange
        // Act
        SUT.onBooleanInstructionResponse(true)
        advanceUntilIdle()
        // Assert
        verify { mockMainDecisions.onCardSelectedForEffect(true) }
    }
} 
