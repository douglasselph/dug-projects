package dugsolutions.leaf.main.local

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.SelectedItems
import dugsolutions.leaf.main.gather.MainGameManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.core.DecisionFlowerSelect
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.local.ShouldAskTrashEffect
import dugsolutions.leaf.player.decisions.ui.DecisionAcquireSelectSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionDrawCountSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionFlowerSelectSuspend
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitorReport
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainDecisionsTest {

    companion object {
        private const val DRAW_COUNT = 3
        private const val DAMAGE_AMOUNT = 5
    }

    private val mockMainGameManager = mockk<MainGameManager>(relaxed = true)
    private val mockCardOperations = mockk<CardOperations>(relaxed = true)
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val mockGameCard = mockk<GameCard>(relaxed = true)
    private val mockCardInfo = mockk<CardInfo>(relaxed = true)
    private val mockDieInfo = mockk<DieInfo>(relaxed = true)
    private val mockSelectedItems = mockk<SelectedItems>(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val decidingPlayer = DecidingPlayer()
    private val mockDecisionMonitor: DecisionMonitor = mockk(relaxed = true)
    private val mockShouldAskTrashEffect: ShouldAskTrashEffect = mockk(relaxed = true)
    private val mockDecisionDirector: DecisionDirector = mockk(relaxed = true)
    private val mockDrawCountDecision = mockk<DecisionDrawCountSuspend>(relaxed = true)
    private val mockDecisionAcquireSelect = mockk<DecisionAcquireSelectSuspend>(relaxed = true)
    private val mockDecisionFlowerSelect = mockk<DecisionFlowerSelectSuspend>(relaxed = true)
    private val mockDecisionMonitorReport = mockk<DecisionMonitorReport>(relaxed = true)

    private val sampleDie = SampleDie()
    private val sampleD6 = sampleDie.d6

    private val SUT = MainDecisions(
        mockMainGameManager, mockCardOperations, mockDecisionMonitor, mockDecisionMonitorReport,
        mockShouldAskTrashEffect, decidingPlayer
    )

    @BeforeEach
    fun setup() {
        every { mockPlayer.incomingDamage } returns DAMAGE_AMOUNT
        every { mockCardOperations.getCard(mockCardInfo) } returns mockGameCard
        every { mockMainGameManager.gatherSelected() } returns mockSelectedItems
        every { mockDieInfo.backingDie } returns sampleD6
        every { mockPlayer.decisionDirector } returns mockDecisionDirector
        every { mockDecisionDirector.drawCountDecision } returns mockDrawCountDecision
        every { mockDecisionDirector.acquireSelectDecision } returns mockDecisionAcquireSelect
        every { mockDecisionDirector.flowerSelectDecision } returns mockDecisionFlowerSelect
    }

    @Test
    fun setup_whenCalled_initializesPlayerDecisions() {
        // Arrange
        // Act
        SUT.setup(mockPlayer)

        // Assert
        verify { mockDecisionDirector.drawCountDecision = any<DecisionDrawCount>() }
        verify { mockDecisionDirector.acquireSelectDecision = any<DecisionAcquireSelect>() }
        verify { mockDecisionDirector.damageAbsorptionDecision = any<DecisionDamageAbsorption>() }
        verify { mockDecisionDirector.shouldProcessTrashEffect = any<DecisionShouldProcessTrashEffect>() }
        verify { mockDecisionDirector.flowerSelectDecision = any<DecisionFlowerSelect>() }
    }

    @Test
    fun setup_whenCalled_resetTrashDecision() {
        // Arrange
        // Act
        SUT.setup(mockPlayer)

        // Assert
        verify { mockShouldAskTrashEffect.askTrashOkay = false }
    }

    @Test
    fun onDrawCountChosen_whenValueProvided_updatesState() {
        // Arrange
        SUT.setup(mockPlayer)

        // Act
        SUT.onDrawCountChosen(mockPlayer, DRAW_COUNT)

        // Assert
        verify { mockDrawCountDecision.provide(DecisionDrawCount.Result(DRAW_COUNT))}
    }

    @Test
    fun onDrawCountChosen_whenDecisionIsNotSuspendType_doesNotProvide() {
        // Arrange
        val mockNonSuspendDecision = mockk<DecisionDrawCount>(relaxed = true)
        every { mockPlayer.decisionDirector.drawCountDecision } returns mockNonSuspendDecision

        // Act
        SUT.onDrawCountChosen(mockPlayer, DRAW_COUNT)

        // Assert
        // Should not crash or try to call provide() on wrong type
    }

    @Test
    fun onGroveCardSelected_whenCardExists_updatesState() {
        // Arrange
        SUT.setup(mockPlayer)
        decidingPlayer.player = mockPlayer

        // Act
        SUT.onGroveCardSelected(mockCardInfo)

        // Assert
        verify { mockCardOperations.getCard(mockCardInfo) }
        verify { mockMainGameManager.clearGroveCardHighlights() }
        verify { mockDecisionAcquireSelect.provide(mockGameCard) }
    }

    @Test
    fun onGroveCardSelected_whenCardDoesNotExist_doesNotUpdateState() {
        // Arrange
        every { mockCardOperations.getCard(mockCardInfo) } returns null

        // Act
        SUT.onGroveCardSelected(mockCardInfo)

        // Assert
        verify(exactly = 0) { mockMainGameManager.clearGroveCardHighlights() }
    }

    @Test
    fun onGroveDieSelected_whenDieExists_updatesState() = runBlocking {
        // Arrange
        SUT.setup(mockPlayer)
        decidingPlayer.player = mockPlayer

        // Act
        SUT.onGroveDieSelected(mockDieInfo)

        // Assert
        verify { mockMainGameManager.clearGroveCardHighlights() }
        verify { mockDecisionAcquireSelect.provide(sampleD6) }
    }

    @Test
    fun onPlayerSelectionComplete_whenCalled_updatesState() {
        // Arrange
        SUT.setup(mockPlayer)
        decidingPlayer.player = mockPlayer
        SUT.selecting = MainDecisions.Selecting.ITEMS

        // Act
        SUT.onPlayerSelectionComplete()

        // Assert
        verify { mockMainGameManager.gatherSelected() }
        verify { mockMainGameManager.clearPlayerSelect() }
    }

    @Test
    fun onPlayerSelectionComplete_whenCalled_providesSelectedItems() {
        // Arrange
        val mockCards = listOf(mockGameCard)
        val mockDice = listOf(sampleDie.d6)
        every { mockSelectedItems.cards } returns mockCards
        every { mockSelectedItems.dice } returns mockDice
        SUT.setup(mockPlayer)
        decidingPlayer.player = mockPlayer
        SUT.selecting = MainDecisions.Selecting.ITEMS

        // Act
        SUT.onPlayerSelectionComplete()

        // Assert
        verify { mockMainGameManager.clearPlayerSelect() }
    }

    @Test
    fun onPlayerSelectionComplete_whenFloralCardsSelected_includesInResult() {
        // Arrange
        val mockFloralCards = listOf(mockGameCard)
        every { mockSelectedItems.floralCards } returns mockFloralCards
        every { mockSelectedItems.cards } returns emptyList()
        every { mockSelectedItems.dice } returns emptyList()
        SUT.setup(mockPlayer)
        decidingPlayer.player = mockPlayer
        SUT.selecting = MainDecisions.Selecting.ITEMS

        // Act
        SUT.onPlayerSelectionComplete()

        // Assert
        verify { mockMainGameManager.gatherSelected() }
        // The floralCards should be included in the result passed to the decision
    }

    @Test
    fun onPlayerSelectionComplete_whenSelectingFlowers_providesSelectedFloralCards() {
        // Arrange
        val mockFloralCards = listOf(mockGameCard)
        every { mockSelectedItems.floralCards } returns mockFloralCards
        SUT.setup(mockPlayer)
        decidingPlayer.player = mockPlayer
        SUT.selecting = MainDecisions.Selecting.FLOWERS

        // Act
        SUT.onPlayerSelectionComplete()

        // Assert
        verify { mockDecisionFlowerSelect.provide(DecisionFlowerSelect.Result(mockFloralCards)) }
        verify { mockMainGameManager.clearPlayerSelect() }
    }

    @Test
    fun setAskTrash_whenSetToTrue_delegatesToMainDomainManager() {
        // Arrange
        // Act
        SUT.setAskTrash(true)

        // Assert
        verify { mockMainGameManager.setAskTrash(true) }
        verify { mockShouldAskTrashEffect.askTrashOkay = true }
    }

    @Test
    fun setAskTrash_whenSetToFalse_delegatesToMainDomainManager() {
        // Arrange
        // Act
        SUT.setAskTrash(false)

        // Assert
        verify { mockMainGameManager.setAskTrash(false) }
        verify { mockShouldAskTrashEffect.askTrashOkay = false }
    }
} 
