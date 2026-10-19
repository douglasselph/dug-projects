package dugsolutions.leaf.game.acquire.cost

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.DieValues
import dugsolutions.leaf.random.die.MissingDieException
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.common.domain.acquire.UsingDice
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplyCostTest {

    private lateinit var mockPlayer: Player
    private lateinit var mockPlayer2: Player
    private lateinit var testDie1: DieValue
    private lateinit var testDie2: DieValue
    private lateinit var dieValues: DieValues
    private lateinit var combination: UsingDice
    private lateinit var dieFactory: DieFactory

    private lateinit var SUT: ApplyCost

    @BeforeEach
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        mockPlayer2 = mockk(relaxed = true)
        
        val randomizer = Randomizer.create()
        dieFactory = DieFactory(randomizer)

        // Create test dice
        testDie1 = DieValue(6, 3)
        testDie2 = DieValue(8, 5)

        // Create DieValues from the test dice
        dieValues = DieValues(listOf(testDie1, testDie2))

        // Create a combination with the DieValues
        combination = UsingDice(
            values = dieValues,
            addToTotal = 0
        )
        
        SUT = ApplyCost()

        every { mockPlayer.discard(any<Die>()) } returns true
        every { mockPlayer.discard(any<DieValue>()) } returns true
        every { mockPlayer2.discard(any<Die>()) } returns true
        every { mockPlayer2.discard(any<DieValue>()) } returns true
    }

    @Test
    fun invoke_discardsAllDice() {
        // Act
        SUT(mockPlayer, combination)

        // Assert
        verify { mockPlayer.discard(testDie1) }
        verify { mockPlayer.discard(testDie2) }
    }

    @Test
    fun invoke_whenDiscardFails_throwsException() {
        // Arrange
        every { mockPlayer.discard(any<DieValue>()) } returns false

        // Act & Assert
        val exception = assertThrows<MissingDieException> {
            SUT(mockPlayer, combination)
        }

        // Verify the exception details
        assertEquals("Could not discard the die $testDie1", exception.message)
    }

    @Test
    fun invoke_callsAcquireItemLambda() {
        // Arrange
        var acquireItemCalled = false
        val acquireItem: (Player) -> Unit = {
            acquireItemCalled = true
        }

        // Act
        SUT(mockPlayer, combination, acquireItem)

        // Assert
        assertTrue(acquireItemCalled)
    }

    @Test
    fun invoke_withDefaultAcquireItem_completesSuccessfully() {
        // Act - using default value for acquireItem
        SUT(mockPlayer, combination)

        // Assert - verify core functionality still runs
        verify { mockPlayer.discard(testDie1) }
        verify { mockPlayer.discard(testDie2) }
    }

    @Test
    fun invoke_withEmptyCombination_onlyCallsAcquireItem() {
        // Arrange
        val emptyDieValues = DieValues(emptyList())
        val emptyCombination = UsingDice(
            values = emptyDieValues,
            addToTotal = 0
        )
        var acquireItemCalled = false
        val acquireItem: (Player) -> Unit = {
            acquireItemCalled = true
        }

        // Act
        SUT(mockPlayer, emptyCombination, acquireItem)

        // Assert
        assertTrue(acquireItemCalled)
        // No dice to discard so verify no discard calls
        verify(exactly = 0) { mockPlayer.discard(any<DieValue>()) }
    }

    @Test
    fun invoke_withMultipleDice_discardsAllDice() {
        // Arrange
        val additionalDie = DieValue(10, 4)
        val multipleDieValues = DieValues(listOf(testDie1, testDie2, additionalDie))
        val multipleCombination = UsingDice(
            values = multipleDieValues,
            addToTotal = 0
        )

        // Act
        SUT(mockPlayer, multipleCombination)

        // Assert
        verify { mockPlayer.discard(testDie1) }
        verify { mockPlayer.discard(testDie2) }
        verify { mockPlayer.discard(additionalDie) }
    }

    @Test
    fun invoke_withDifferentPlayer_worksWithMockPlayer2() {
        // Act
        SUT(mockPlayer2, combination)

        // Assert
        verify { mockPlayer2.discard(testDie1) }
        verify { mockPlayer2.discard(testDie2) }
    }

    @Test
    fun invoke_withAcquireItemLambda_callsLambdaWithCorrectPlayer() {
        // Arrange
        var receivedPlayer: Player? = null
        val acquireItem: (Player) -> Unit = { player ->
            receivedPlayer = player
        }

        // Act
        SUT(mockPlayer, combination, acquireItem)

        // Assert
        assertEquals(mockPlayer, receivedPlayer)
    }

    @Test
    fun invoke_whenFirstDieDiscardFails_throwsExceptionWithFirstDie() {
        // Arrange
        every { mockPlayer.discard(testDie1) } returns false

        // Act & Assert
        val exception = assertThrows<MissingDieException> {
            SUT(mockPlayer, combination)
        }

        assertEquals("Could not discard the die $testDie1", exception.message)
    }

    @Test
    fun invoke_whenSecondDieDiscardFails_throwsExceptionWithSecondDie() {
        // Arrange
        every { mockPlayer.discard(testDie1) } returns true
        every { mockPlayer.discard(testDie2) } returns false

        // Act & Assert
        val exception = assertThrows<MissingDieException> {
            SUT(mockPlayer, combination)
        }

        assertEquals("Could not discard the die $testDie2", exception.message)
    }
}