package dugsolutions.leaf.game.acquire

import dugsolutions.leaf.components.die.SampleDie
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

    private lateinit var mockSelectPossibleCards: SelectPossibleCards
    private lateinit var mockAcquireItem: AcquireItem
    private lateinit var mockManageAcquiredFloralTypes: ManageAcquiredFloralTypes
    private lateinit var mockPlayer: Player
    private lateinit var sampleDie: SampleDie

    private lateinit var SUT: HandleGroveAcquisition

    @BeforeEach
    fun setup() {
        mockSelectPossibleCards = mockk(relaxed = true)
        mockAcquireItem = mockk(relaxed = true)
        mockManageAcquiredFloralTypes = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        sampleDie = SampleDie()

        SUT = HandleGroveAcquisition(
            mockSelectPossibleCards,
            mockAcquireItem,
            mockManageAcquiredFloralTypes
        )
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

        val possibleCards1 = listOf(mockk<dugsolutions.leaf.components.GameCard>())
        val possibleCards2 = listOf(mockk<dugsolutions.leaf.components.GameCard>())
        val possibleCards3 = listOf(mockk<dugsolutions.leaf.components.GameCard>())

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

        val emptyCards = emptyList<dugsolutions.leaf.components.GameCard>()
        val possibleCards = listOf(mockk<dugsolutions.leaf.components.GameCard>())

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
