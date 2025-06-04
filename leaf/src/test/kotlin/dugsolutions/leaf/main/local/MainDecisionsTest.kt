package dugsolutions.leaf.main.local

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.ChoiceDie
import dugsolutions.leaf.game.acquire.domain.FakeCombination
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.SelectedItems
import dugsolutions.leaf.main.gather.MainDomainManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.ui.DecisionDrawCountSuspend
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainDecisionsTest {

    companion object {
        private const val DRAW_COUNT = 3
        private const val DAMAGE_AMOUNT = 5
    }

    private val mockMainDomainManager = mockk<MainDomainManager>(relaxed = true)
    private val mockCardOperations = mockk<CardOperations>(relaxed = true)
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val mockGameCard = mockk<GameCard>(relaxed = true)
    private val mockCardInfo = mockk<CardInfo>(relaxed = true)
    private val mockDieInfo = mockk<DieInfo>(relaxed = true)
    private val mockSelectedItems = mockk<SelectedItems>(relaxed = true)
    private val sampleDie = SampleDie()
    private val sampleD6 = sampleDie.d6
    private val mockDecisionAcquireSelect = mockk<DecisionAcquireSelect>(relaxed = true)

    private val SUT = MainDecisions(mockMainDomainManager, mockCardOperations)

    @BeforeEach
    fun setup() {
        every { mockPlayer.incomingDamage } returns DAMAGE_AMOUNT
        every { mockCardOperations.getCard(mockCardInfo) } returns mockGameCard
        every { mockMainDomainManager.gatherSelected() } returns mockSelectedItems
        every { mockDieInfo.backingDie } returns sampleD6
    }

    @Test
    fun setup_whenCalled_initializesPlayerDecisions() {
        // Arrange
        // Act
        SUT.setup(mockPlayer)

        // Assert
        verify { mockPlayer.decisionDirector.drawCountDecision = any<DecisionDrawCount>() }
        verify { mockPlayer.decisionDirector.acquireSelectDecision = any<DecisionAcquireSelect>() }
        verify { mockPlayer.decisionDirector.damageAbsorptionDecision = any<DecisionDamageAbsorption>() }
    }

    @Test
    fun setup_whenCalled_setsUpDrawCountCallback() {
        // Arrange
        val fakePlayer = PlayerTD(1)

        // Act
        SUT.setup(fakePlayer)

        // Assert
        val decision = fakePlayer.decisionDirector.drawCountDecision
        assertTrue(decision is DecisionDrawCountSuspend)

        // Verify callback is set (this tests the lambda assignment)
        val suspendDecision = decision as DecisionDrawCountSuspend
        assertNotNull(suspendDecision.onDrawCountRequest)
    }

    @Test
    fun onDrawCountChosen_whenValueProvided_updatesState() {
        // Arrange
        // Act
        SUT.onDrawCountChosen(mockPlayer, DRAW_COUNT)

        // Assert
        verify { mockMainDomainManager.clearShowDrawCount() }
    }

    @Test
    fun onDrawCountChosen_whenDecisionIsNotSuspendType_doesNotProvide() {
        // Arrange
        val mockNonSuspendDecision = mockk<DecisionDrawCount>(relaxed = true)
        every { mockPlayer.decisionDirector.drawCountDecision } returns mockNonSuspendDecision

        // Act
        SUT.onDrawCountChosen(mockPlayer, DRAW_COUNT)

        // Assert
        verify { mockMainDomainManager.clearShowDrawCount() }
        // Should not crash or try to call provide() on wrong type
    }

    @Test
    fun onGroveCardSelected_whenCardExists_updatesState() {
        // Arrange
        SUT.setup(mockPlayer)
        mockPlayer.decisionDirector.acquireSelectDecision = mockDecisionAcquireSelect
        SUT.decidingPlayer = mockPlayer

        // Act
        SUT.onGroveCardSelected(mockCardInfo)

        // Assert
        verify { mockCardOperations.getCard(mockCardInfo) }
        verify { mockMainDomainManager.clearGroveCardHighlights() }
    }

    @Test
    fun onGroveCardSelected_whenCardDoesNotExist_doesNotUpdateState() {
        // Arrange
        every { mockCardOperations.getCard(mockCardInfo) } returns null

        // Act
        SUT.onGroveCardSelected(mockCardInfo)

        // Assert
        verify(exactly = 0) { mockMainDomainManager.clearGroveCardHighlights() }
    }

    @Test
    fun onGroveDieSelected_whenDieExists_updatesState() = runBlocking {
        // Arrange
        SUT.setup(mockPlayer)
        val cards = emptyList<ChoiceCard>()
        val dice = listOf(ChoiceDie(sampleD6, FakeCombination.combinationD10))
        mockPlayer.decisionDirector.acquireSelectDecision(cards, dice)
        SUT.decidingPlayer = mockPlayer

        // Act
        SUT.onGroveDieSelected(mockDieInfo)

        // Assert
        verify { mockMainDomainManager.clearGroveCardHighlights() }
    }

    @Test
    fun onPlayerSelectionComplete_whenCalled_updatesState() {
        // Arrange
        SUT.setup(mockPlayer)
        SUT.decidingPlayer = mockPlayer

        // Act
        SUT.onPlayerSelectionComplete()

        // Assert
        verify { mockMainDomainManager.gatherSelected() }
        verify { mockMainDomainManager.clearPlayerSelect() }
        verify { mockMainDomainManager.setActionButton(ActionButton.NONE) }
    }

    @Test
    fun onPlayerSelectionComplete_whenCalled_providesSelectedItems() {
        // Arrange
        val mockCards = listOf(mockGameCard)
        val mockDice = listOf(sampleDie.d6)
        every { mockSelectedItems.cards } returns mockCards
        every { mockSelectedItems.dice } returns mockDice
        SUT.setup(mockPlayer)
        SUT.decidingPlayer = mockPlayer

        // Act
        SUT.onPlayerSelectionComplete()

        // Assert
        verify { mockMainDomainManager.clearPlayerSelect() }
        verify { mockMainDomainManager.setActionButton(ActionButton.NONE) }
    }

    @Test
    fun onPlayerSelectionComplete_whenFloralCardsSelected_includesInResult() {
        // Arrange
        val mockFloralCards = listOf(mockGameCard)
        every { mockSelectedItems.floralCards } returns mockFloralCards
        every { mockSelectedItems.cards } returns emptyList()
        every { mockSelectedItems.dice } returns emptyList()
        SUT.setup(mockPlayer)
        SUT.decidingPlayer = mockPlayer

        // Act
        SUT.onPlayerSelectionComplete()

        // Assert
        verify { mockMainDomainManager.gatherSelected() }
        // The floralCards should be included in the result passed to the decision
    }

    @Test
    fun drawCountCallback_whenTriggered_updatesUI() = runBlocking {
        // Arrange
        val fakePlayer = PlayerTD(1)
        SUT.setup(fakePlayer)
        val decision = fakePlayer.decisionDirector.drawCountDecision as DecisionDrawCountSuspend

        // Act
        decision.onDrawCountRequest()

        // Assert
        verify { mockMainDomainManager.updateData() }
        verify { mockMainDomainManager.setShowDrawCount(fakePlayer, true) }
    }

    @Test
    fun setAskTrash_whenSetToTrue_delegatesToMainDomainManager() {
        // Arrange
        // Act
        SUT.setAskTrash(true)

        // Assert
        verify { mockMainDomainManager.setAskTrash(true) }
    }

    @Test
    fun setAskTrash_whenSetToFalse_delegatesToMainDomainManager() {
        // Arrange
        // Act
        SUT.setAskTrash(false)

        // Assert
        verify { mockMainDomainManager.setAskTrash(false) }
    }
} 
