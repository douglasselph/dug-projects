package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.Player
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecisionShouldTargetPlayerSuspendTest {

    private lateinit var mockTarget: Player
    private lateinit var SUT: DecisionShouldTargetPlayerSuspend

    @BeforeEach
    fun setup() {
        mockTarget = mockk(relaxed = true)
        SUT = DecisionShouldTargetPlayerSuspend()
    }

    @Test
    fun invoke_whenCalled_throwsUnsupportedOperationException() = runBlocking {
        // Arrange
        val amount = 5

        // Act & Assert
        val exception = org.junit.jupiter.api.assertThrows<UnsupportedOperationException> {
            SUT.invoke(mockTarget, amount)
        }
        
        assertEquals("Use invokeSuspend() instead of invoke() for suspend-based decisions", exception.message)
    }

    @Test
    fun invokeSuspend_whenWaitingForDecision_suspends() = runBlocking {
        // Arrange
        val amount = 3
        var result: Boolean? = null
        var completed = false

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            result = SUT.invokeSuspend(mockTarget, amount)
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
    fun invokeSuspend_whenTrueDecisionProvided_returnsTrue() = runBlocking {
        // Arrange
        val amount = 5
        val expectedResult = true
        var actualResult: Boolean? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT.invokeSuspend(mockTarget, amount)
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
    fun invokeSuspend_whenFalseDecisionProvided_returnsFalse() = runBlocking {
        // Arrange
        val amount = 2
        val expectedResult = false
        var actualResult: Boolean? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT.invokeSuspend(mockTarget, amount)
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
    fun invokeSuspend_withDifferentTargetsAndAmounts_handlesCorrectly() = runBlocking {
        // Arrange
        val testCases = listOf(
            Triple(mockTarget, 1, true),
            Triple(mockTarget, 5, false),
            Triple(mockTarget, 10, true)
        )
        val actualResults = mutableListOf<Boolean>()

        // Act & Assert
        for ((target, amount, expectedResult) in testCases) {
            val waitingJob = launch {
                actualResults.add(SUT.invokeSuspend(target, amount))
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
        assertEquals(testCases.map { it.third }, actualResults)
    }

    @Test
    fun provide_whenNotWaiting_doesNothing() = runBlocking {
        // Arrange
        val result = true

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
            SUT.invokeSuspend(mockTarget, 3)
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
            SUT.invokeSuspend(mockTarget, 4)
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
        val results = listOf(true, false, true, false)
        val actualResults = mutableListOf<Boolean>()

        // Act & Assert
        for (expectedResult in results) {
            val waitingJob = launch {
                actualResults.add(SUT.invokeSuspend(mockTarget, 3))
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
