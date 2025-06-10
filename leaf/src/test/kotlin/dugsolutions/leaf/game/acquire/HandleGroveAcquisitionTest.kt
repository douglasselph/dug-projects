package dugsolutions.leaf.game.acquire

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.game.turn.select.SelectPossibleCards
import dugsolutions.leaf.player.Player
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleGroveAcquisitionTest {

    private val mockSelectPossibleCards: SelectPossibleCards = mockk(relaxed = true)
    private val mockAcquireItem: AcquireItem = mockk(relaxed = true)
    private val mockManageAcquiredFloralTypes: ManageAcquiredFloralTypes = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val mockPlayer: Player = mockk(relaxed = true)
    private lateinit var sampleDie: SampleDie

    private val SUT: HandleGroveAcquisition = HandleGroveAcquisition(
        mockSelectPossibleCards,
        mockAcquireItem,
        mockManageAcquiredFloralTypes,
        mockChronicle
    )

    @BeforeEach
    fun setup() {
        sampleDie = SampleDie()
        coEvery { mockAcquireItem(any(), any()) } returns true
    }

    @Test
    fun invoke_whenNoDiceInHand_onlyClearsTypes() = runBlocking {
        // Arrange
        every { mockPlayer.diceInHand.isNotEmpty() } returns false

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockManageAcquiredFloralTypes.clear() }
        verify(exactly = 0) { mockSelectPossibleCards() }
        coVerify(exactly = 0) { mockAcquireItem(any(), any()) }
    }

    @Test
    fun invoke_withDiceInHand_loopsUntilNoDice() = runBlocking {
        // Arrange

        // Set up to run loop 3 times
        every { mockPlayer.diceInHand.isNotEmpty() } returnsMany listOf(true, true, true, false)

        val possibleCards1 = listOf(mockk<GameCard>())
        val possibleCards2 = listOf(mockk<GameCard>())
        val possibleCards3 = listOf(mockk<GameCard>())

        every { mockSelectPossibleCards() } returnsMany listOf(possibleCards1, possibleCards2, possibleCards3)

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockManageAcquiredFloralTypes.clear() }
        verify(exactly = 3) { mockSelectPossibleCards() }
        coVerify { mockAcquireItem(mockPlayer, possibleCards1) }
        coVerify { mockAcquireItem(mockPlayer, possibleCards2) }
        coVerify { mockAcquireItem(mockPlayer, possibleCards3) }
    }

    @Test
    fun invoke_withEmptyPossibleCards_stillContinuesLoop() = runBlocking {
        // Arrange

        // Set up to run loop 2 times
        every { mockPlayer.diceInHand.isNotEmpty() } returnsMany listOf(true, true, false)

        val emptyCards = emptyList<GameCard>()
        val possibleCards = listOf(mockk<GameCard>())

        every { mockSelectPossibleCards() } returnsMany listOf(emptyCards, possibleCards)

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockManageAcquiredFloralTypes.clear() }
        verify(exactly = 2) { mockSelectPossibleCards() }
        coVerify { mockAcquireItem(mockPlayer, emptyCards) }
        coVerify { mockAcquireItem(mockPlayer, possibleCards) }
    }

    @Test
    fun invoke_whenLoopExceedsFailsafe_throwsException() = runBlocking {
        // Arrange
        // Set up to always return true for isNotEmpty to force infinite loop
        every { mockPlayer.diceInHand.isNotEmpty() } returns true
        var gotException: Exception? = null

        // Act
        try {
            SUT(mockPlayer)
        } catch (ex: Exception) {
            gotException = ex
        }

        // Assert
        assertTrue(gotException is HandleGroveAcquisition.NoEndInSiteException)
    }
} 
