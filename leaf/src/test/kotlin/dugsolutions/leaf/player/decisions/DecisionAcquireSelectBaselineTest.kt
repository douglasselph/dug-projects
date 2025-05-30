package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.game.acquire.evaluator.AcquireCardEvaluator
import dugsolutions.leaf.game.acquire.evaluator.AcquireDieEvaluator
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.baseline.DecisionAcquireSelectBaseline
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DecisionAcquireSelectBaselineTest {

    private lateinit var player: Player
    private lateinit var mockBestCard: AcquireCardEvaluator.Choice
    private lateinit var mockBestDie: AcquireDieEvaluator.BestChoice

    private lateinit var SUT: DecisionAcquireSelectBaseline

    @BeforeEach
    fun setup() {
        player = mockk(relaxed = true)
        mockBestCard = mockk(relaxed = true)
        mockBestDie = mockk(relaxed = true)

        SUT = DecisionAcquireSelectBaseline(player)
    }

    @Test
    fun invoke_whenCardScoreLowerAndBestCardExists_returnsCard() {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 3
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        
        // Act
        val result = SUT(mockBestCard, mockBestDie)
        
        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Card(mockBestCard), result)
    }

    @Test
    fun invoke_whenCardScoreLowerButNoBestCard_returnsDie() {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 3
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        
        // Act
        val result = SUT(null, mockBestDie)
        
        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenDieScoreLower_returnsDie() {
        // Arrange
        every { player.totalCardCount } returns 3
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        
        // Act
        val result = SUT(mockBestCard, mockBestDie)
        
        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenScoresEqual_returnsDie() {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        
        // Act
        val result = SUT(mockBestCard, mockBestDie)
        
        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenPreferenceCardNegative_biasesTowardsCards() {
        // Arrange
        every { player.totalCardCount } returns 3
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = -2  // Makes card score = 1
        SUT.preferenceDie = 0    // Makes die score = 2
        
        // Act
        val result = SUT(mockBestCard, mockBestDie)
        
        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Card(mockBestCard), result)
    }

    @Test
    fun invoke_whenPreferenceDieNegative_biasesTowardsDice() {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 3
        SUT.preferenceCard = 0    // Makes card score = 2
        SUT.preferenceDie = -2    // Makes die score = 1
        
        // Act
        val result = SUT(mockBestCard, mockBestDie)
        
        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenBothPreferencesSet_usesCombinedScores() {
        // Arrange
        every { player.totalCardCount } returns 3
        every { player.totalDiceCount } returns 3
        SUT.preferenceCard = -1  // Makes card score = 2
        SUT.preferenceDie = -2   // Makes die score = 1
        
        // Act
        val result = SUT(mockBestCard, mockBestDie)
        
        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenNoBestDieAndCardScoreHigher_returnsNone() {
        // Arrange
        every { player.totalCardCount } returns 3
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        
        // Act
        val result = SUT(mockBestCard, null)
        
        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.None, result)
    }

    @Test
    fun invoke_whenBothBestChoicesNull_returnsNone() {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        
        // Act
        val result = SUT(null, null)
        
        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.None, result)
    }
} 
