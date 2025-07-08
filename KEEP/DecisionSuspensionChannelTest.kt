package dugsolutions.leaf.player.decisions.local.monitor

import dugsolutions.leaf.player.Player
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class DecisionSuspensionChannelTest {

    private val mockPlayer: Player = mockk(relaxed = true)
    private val monitor: DecisionMonitor = DecisionMonitor()
    private val report: DecisionMonitorReport = mockk(relaxed = true)
    private val channel: DecisionSuspensionChannel<Int> = DecisionSuspensionChannel(monitor, report)

    @BeforeEach
    fun setup() {
    }

    @Test
    fun waitForDecision_whenWaitingForValue_updatesMonitorAndReturnsValue() = runBlocking {
        // Arrange
        val expectedValue = 42
        val decisionId = DecisionID.DRAW_COUNT(mockPlayer)
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
        val decisionId = DecisionID.DRAW_COUNT(mockPlayer)

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
        val decisionId = DecisionID.DRAW_COUNT(mockPlayer)

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
