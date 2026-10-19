package dugsolutions.leaf.player.decisions.ui

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecisionTaskHandlerTest {

    private lateinit var SUT: DecisionTaskHandler<String>

    @BeforeEach
    fun setup() {
        SUT = DecisionTaskHandler()
    }

    @Test
    fun waitForDecision_whenNoValueProvided_suspends() = runBlocking {
        // Arrange
        var result: String? = null
        var completed = false

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            result = SUT.waitForDecision()
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
    fun waitForDecision_whenValueProvided_returnsValue() = runBlocking {
        // Arrange
        val expectedValue = "test result"
        var actualResult: String? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT.waitForDecision()
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(50)

        // Provide the value
        SUT.provideDecision(expectedValue)

        // Wait for the waiting coroutine to complete
        waitingJob.join()

        // Assert
        assertEquals(expectedValue, actualResult)
        assertFalse(SUT.isWaiting())
    }

    @Test
    fun provideDecision_whenNotWaiting_doesNothing() = runBlocking {
        // Arrange
        val value = "test value"

        // Act
        SUT.provideDecision(value)

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
            SUT.waitForDecision()
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(50)

        // Assert
        assertTrue(SUT.isWaiting())

        // Cleanup
        waitingJob.cancel()
    }

    @Test
    fun cancelDecision_whenWaiting_cancelsAndClears() = runBlocking {
        // Arrange
        val waitingJob = launch {
            SUT.waitForDecision()
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(50)

        // Act
        SUT.cancelDecision()

        // Assert
        assertFalse(SUT.isWaiting())

        // Cleanup
        waitingJob.cancel()
    }

    @Test
    fun cancelDecision_whenNotWaiting_doesNothing() = runBlocking {
        // Act
        SUT.cancelDecision()

        // Assert
        assertFalse(SUT.isWaiting())
    }

    @Test
    fun multipleDecisions_handlesSequentially() = runBlocking {
        // Arrange
        val values = listOf("first", "second", "third")
        val results = mutableListOf<String>()

        // Act & Assert
        for (expectedValue in values) {
            val waitingJob = launch {
                results.add(SUT.waitForDecision())
            }

            // Wait for the coroutine to reach the suspension point
            kotlinx.coroutines.delay(50)

            // Provide the value
            SUT.provideDecision(expectedValue)

            // Wait for the waiting coroutine to complete
            waitingJob.join()

            // Verify the result
            assertEquals(expectedValue, results.last())
            assertFalse(SUT.isWaiting())
        }

        // Verify all results
        assertEquals(values, results)
    }
}
