package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.ChoiceDie
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DecisionAcquireSelectBaselineTest {

    private lateinit var player: Player
    private val acquireCardEvaluator = mockk<AcquireCardEvaluator>(relaxed = true)
    private val acquireDieEvaluator = mockk<AcquireDieEvaluator>(relaxed = true)
    private val mockBestCard = mockk<ChoiceCard>()
    private val mockBestDie = mockk<ChoiceDie>()

    private lateinit var SUT: DecisionAcquireSelectBaseline

    @BeforeEach
    fun setup() {
        player = mockk(relaxed = true)
        SUT = DecisionAcquireSelectBaseline(player, acquireCardEvaluator, acquireDieEvaluator)
    }

    @Test
    fun invoke_whenCardScoreLowerAndBestCardExists_returnsCard() = runBlocking {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 3
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        every { acquireCardEvaluator(any()) } returns mockBestCard
        every { acquireDieEvaluator(any()) } returns mockBestDie

        // Act
        val result = SUT(listOf(mockBestCard), listOf(mockBestDie))

        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Card(mockBestCard), result)
    }

    @Test
    fun invoke_whenCardScoreLowerButNoBestCard_returnsDie() = runBlocking {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 3
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        every { acquireCardEvaluator(any()) } returns null
        every { acquireDieEvaluator(any()) } returns mockBestDie

        // Act
        val result = SUT(listOf(mockBestCard), listOf(mockBestDie))

        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenDieScoreLower_returnsDie() = runBlocking {
        // Arrange
        every { player.totalCardCount } returns 3
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        every { acquireCardEvaluator(any()) } returns mockBestCard
        every { acquireDieEvaluator(any()) } returns mockBestDie

        // Act
        val result = SUT(listOf(mockBestCard), listOf(mockBestDie))

        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenScoresEqual_returnsDie() = runBlocking {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        every { acquireCardEvaluator(any()) } returns mockBestCard
        every { acquireDieEvaluator(any()) } returns mockBestDie

        // Act
        val result = SUT(listOf(mockBestCard), listOf(mockBestDie))

        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenPreferenceCardNegative_biasesTowardsCards() = runBlocking {
        // Arrange
        every { player.totalCardCount } returns 3
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = -2  // Makes card score = 1
        SUT.preferenceDie = 0    // Makes die score = 2
        every { acquireCardEvaluator(any()) } returns mockBestCard
        every { acquireDieEvaluator(any()) } returns mockBestDie

        // Act
        val result = SUT(listOf(mockBestCard), listOf(mockBestDie))

        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Card(mockBestCard), result)
    }

    @Test
    fun invoke_whenPreferenceDieNegative_biasesTowardsDice() = runBlocking {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 3
        SUT.preferenceCard = 0    // Makes card score = 2
        SUT.preferenceDie = -2    // Makes die score = 1
        every { acquireCardEvaluator(any()) } returns mockBestCard
        every { acquireDieEvaluator(any()) } returns mockBestDie

        // Act
        val result = SUT(listOf(mockBestCard), listOf(mockBestDie))

        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenBothPreferencesSet_usesCombinedScores() = runBlocking {
        // Arrange
        every { player.totalCardCount } returns 3
        every { player.totalDiceCount } returns 3
        SUT.preferenceCard = -1  // Makes card score = 2
        SUT.preferenceDie = -2   // Makes die score = 1
        every { acquireCardEvaluator(any()) } returns mockBestCard
        every { acquireDieEvaluator(any()) } returns mockBestDie

        // Act
        val result = SUT(listOf(mockBestCard), listOf(mockBestDie))

        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.Die(mockBestDie), result)
    }

    @Test
    fun invoke_whenNoBestDieAndCardScoreHigher_returnsNone() = runBlocking {
        // Arrange
        every { player.totalCardCount } returns 3
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        every { acquireCardEvaluator(any()) } returns mockBestCard
        every { acquireDieEvaluator(any()) } returns null

        // Act
        val result = SUT(listOf(mockBestCard), listOf(mockBestDie))

        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.None, result)
    }

    @Test
    fun invoke_whenBothBestChoicesNull_returnsNone() = runBlocking {
        // Arrange
        every { player.totalCardCount } returns 2
        every { player.totalDiceCount } returns 2
        SUT.preferenceCard = 0
        SUT.preferenceDie = 0
        every { acquireCardEvaluator(any()) } returns null
        every { acquireDieEvaluator(any()) } returns null

        // Act
        val result = SUT(listOf(mockBestCard), listOf(mockBestDie))

        // Assert
        assertEquals(DecisionAcquireSelect.BuyItem.None, result)
    }
} 
