package dugsolutions.leaf.main.local

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.SampleDie
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainDecisionsTest {

    companion object {
        private const val DRAW_COUNT = 3
        private const val DAMAGE_AMOUNT = 5
    }

    private val mockMainDomainManager = mockk<MainDomainManager>(relaxed = true)
    private val mockCardOperations = mockk<CardOperations>(relaxed = true)
    private val fakePlayer = PlayerTD(1)
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val mockGameCard = mockk<GameCard>(relaxed = true)
    private val mockCardInfo = mockk<CardInfo>(relaxed = true)
    private val mockDieInfo = mockk<DieInfo>(relaxed = true)
    private val mockSelectedItems = mockk<SelectedItems>(relaxed = true)
    private val sampleDie = SampleDie()

    private val SUT = MainDecisions(mockMainDomainManager, mockCardOperations)

    @BeforeEach
    fun setup() {
        every { fakePlayer.incomingDamage } returns DAMAGE_AMOUNT
        every { mockCardOperations.getCard(mockCardInfo) } returns mockGameCard
        every { mockMainDomainManager.gatherSelected() } returns mockSelectedItems
        every { mockDieInfo.backingDie } returns sampleDie.d6
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
    fun onDrawCountChosen_whenValueProvided_updatesState() {
        // Arrange
        // Act
        SUT.onDrawCountChosen(DRAW_COUNT)

        // Assert
        verify { mockMainDomainManager.clearShowDrawCount() }
    }

    @Test
    fun onGroveCardSelected_whenCardExists_updatesState() {
        // Arrange
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
    fun onGroveDieSelected_whenDieExists_updatesState() {
        // Arrange
        // Act
        SUT.onGroveDieSelected(mockDieInfo)

        // Assert
        verify { mockMainDomainManager.clearGroveCardHighlights() }
    }

    @Test
    fun onGroveDieSelected_whenDieDoesNotExist_doesNotUpdateState() {
        // Arrange
        every { mockDieInfo.backingDie } returns null

        // Act
        SUT.onGroveDieSelected(mockDieInfo)

        // Assert
        verify(exactly = 0) { mockMainDomainManager.clearGroveCardHighlights() }
    }

    @Test
    fun onPlayerSelectionComplete_whenCalled_updatesState() {
        // Arrange
        // Act
        SUT.onPlayerSelectionComplete()

        // Assert
        verify { mockMainDomainManager.gatherSelected() }
        verify { mockMainDomainManager.clearAllowPlayerItemSelect() }
        verify { mockMainDomainManager.setActionButton(ActionButton.NONE) }
    }

    @Test
    fun onPlayerSelectionComplete_whenCalled_providesSelectedItems() {
        // Arrange
        val mockCards = listOf(mockGameCard)
        val mockDice = listOf(sampleDie.d6)
        every { mockSelectedItems.cards } returns mockCards
        every { mockSelectedItems.dice } returns mockDice
        SUT.setup(fakePlayer)

        // Act
        SUT.onPlayerSelectionComplete()

        // Assert
        // TODO: This is hard.
    }
} 
