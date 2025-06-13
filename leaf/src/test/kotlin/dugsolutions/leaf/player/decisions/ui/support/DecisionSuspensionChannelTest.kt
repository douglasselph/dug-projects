package dugsolutions.leaf.player.decisions.ui.support

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class DecisionSuspensionChannelTest {

    private lateinit var monitor: DecisionMonitor
    private lateinit var channel: DecisionSuspensionChannel<Int>

    @BeforeEach
    fun setup() {
        monitor = DecisionMonitor()
        channel = DecisionSuspensionChannel(monitor)
    }

    @Test
    fun waitForDecision_whenWaitingForValue_updatesMonitorAndReturnsValue() = runBlocking {
        // Arrange
        val expectedValue = 42
        val decisionId = DecisionID.DRAW_COUNT
        var actualResult: Int? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = channel.waitForDecision(decisionId)
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(100)

        // Verify monitor state was updated
        assertEquals(decisionId, monitor.currentlyWaitingFor)

        // Provide the value
        channel.provideDecision(expectedValue)

        // Wait for the waiting coroutine to complete
        waitingJob.join()

        // Assert
        assertEquals(expectedValue, actualResult)
        assertNull(monitor.currentlyWaitingFor)
    }

    @Test
    fun waitForDecision_whenValueProvidedFirst_returnsValueImmediately() = runBlocking {
        // Arrange
        val expectedValue = 42
        val decisionId = DecisionID.DRAW_COUNT

        // Act - Provide value before waiting
        channel.provideDecision(expectedValue)

        // Start waiting
        val result = channel.waitForDecision(decisionId)

        // Assert
        assertEquals(expectedValue, result)
        assertNull(monitor.currentlyWaitingFor)
    }

    @Test
    fun waitForDecision_whenCalledMultipleTimes_returnsCorrectValuesInSequence() = runBlocking {
        // Arrange
        val values = listOf(1, 2, 3)
        val decisionId = DecisionID.DRAW_COUNT

        // Act & Assert
        for (expectedValue in values) {
            var actualResult: Int? = null
            val waitingJob = launch {
                actualResult = channel.waitForDecision(decisionId)
            }

            // Wait for the coroutine to reach the suspension point
            kotlinx.coroutines.delay(100)

            // Verify monitor state was updated
            assertEquals(decisionId, monitor.currentlyWaitingFor)

            // Now it's safe to provide the value since waitForDecision has been called
            channel.provideDecision(expectedValue)
            
            waitingJob.join()
            assertEquals(expectedValue, actualResult)
            assertNull(monitor.currentlyWaitingFor)
        }
    }
} 
