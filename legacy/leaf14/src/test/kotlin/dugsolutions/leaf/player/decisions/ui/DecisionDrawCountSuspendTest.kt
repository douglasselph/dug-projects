package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecisionDrawCountSuspendTest {

    private lateinit var mockPlayer: Player
    private lateinit var SUT: DecisionDrawCountSuspend

    @BeforeEach
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        SUT = DecisionDrawCountSuspend()
    }

    @Test
    fun invoke_whenWaitingForDecision_suspends() = runBlocking {
        // Arrange
        var result: DecisionDrawCount.Result? = null
        var completed = false

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            result = SUT(mockPlayer)
            completed = true
        }

        // Wait a bit to ensure it's suspended
        kotlinx.coroutines.delay(50)

        // Assert
        assertTrue(SUT.isWaiting())
        assertNull(result)
        assertFalse(completed)

        // Cleanup
        waitingJob.cancel()
    }

    @Test
    fun invoke_whenDecisionProvided_returnsResult() = runBlocking {
        // Arrange
        val expectedResult = DecisionDrawCount.Result(count = 3)
        var actualResult: DecisionDrawCount.Result? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT(mockPlayer)
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(50)

        // Provide the decision
        SUT.provide(expectedResult)

        // Wait for the waiting coroutine to complete
        waitingJob.join()

        // Assert
        assertEquals(expectedResult, actualResult)
        assertFalse(SUT.isWaiting())
    }

    @Test
    fun provide_whenNotWaiting_doesNothing() = runBlocking {
        // Arrange
        val result = DecisionDrawCount.Result(count = 2)

        // Act
        SUT.provide(result)

        // Assert
        assertFalse(SUT.isWaiting())
    }

    @Test
    fun isWaiting_whenNotWaiting_returnsFalse() = runBlocking {
        // Assert
        assertFalse(SUT.isWaiting())
    }

    @Test
    fun isWaiting_whenWaiting_returnsTrue() = runBlocking {
        // Arrange
        val waitingJob = launch {
            SUT(mockPlayer)
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(50)

        // Assert
        assertTrue(SUT.isWaiting())

        // Cleanup
        waitingJob.cancel()
    }

    @Test
    fun cancel_whenWaiting_cancelsAndClears() = runBlocking {
        // Arrange
        val waitingJob = launch {
            SUT(mockPlayer)
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(50)

        // Act
        SUT.cancel()

        // Assert
        assertFalse(SUT.isWaiting())

        // Cleanup
        waitingJob.cancel()
    }

    @Test
    fun multipleDecisions_handlesSequentially() = runBlocking {
        // Arrange
        val results = listOf(
            DecisionDrawCount.Result(count = 1),
            DecisionDrawCount.Result(count = 2),
            DecisionDrawCount.Result(count = 3)
        )
        val actualResults = mutableListOf<DecisionDrawCount.Result>()

        // Act & Assert
        for (expectedResult in results) {
            val waitingJob = launch {
                actualResults.add(SUT(mockPlayer))
            }

            // Wait for the coroutine to reach the suspension point
            kotlinx.coroutines.delay(50)

            // Provide the decision
            SUT.provide(expectedResult)

            // Wait for the waiting coroutine to complete
            waitingJob.join()

            // Verify the result
            assertEquals(expectedResult, actualResults.last())
            assertFalse(SUT.isWaiting())
        }

        // Verify all results
        assertEquals(results, actualResults)
    }
}
