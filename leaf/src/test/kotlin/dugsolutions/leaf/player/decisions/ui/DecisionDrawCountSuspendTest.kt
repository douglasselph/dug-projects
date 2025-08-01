package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitorReport
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DecisionDrawCountSuspendTest {

    private val mockPlayer: Player = mockk(relaxed = true)
    private val monitor: DecisionMonitor = DecisionMonitor()
    private val mockDecisionMonitorReport = mockk<DecisionMonitorReport>(relaxed = true)
    private val SUT: DecisionDrawCountSuspend = DecisionDrawCountSuspend(monitor, mockDecisionMonitorReport)

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_whenWaitingForDecision_returnsProvidedValue() = runBlocking {
        // Arrange
        val expectedResult = DecisionDrawCount.Result(3)
        var actualResult: DecisionDrawCount.Result? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT(mockPlayer)
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(100)

        // Verify monitor state was updated
        assertEquals(DecisionID.DRAW_COUNT(mockPlayer), monitor.currentlyWaitingFor)

        // Provide the value
        SUT.provide(expectedResult)

        // Wait for the waiting coroutine to complete
        waitingJob.join()

        // Assert
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun invoke_whenCalledMultipleTimes_returnsCorrectValuesInSequence() = runBlocking {
        // Arrange
        val expectedResults = listOf(
            DecisionDrawCount.Result(1),
            DecisionDrawCount.Result(2),
            DecisionDrawCount.Result(3)
        )

        // Act & Assert
        for (expectedResult in expectedResults) {
            var actualResult: DecisionDrawCount.Result? = null
            val waitingJob = launch {
                actualResult = SUT(mockPlayer)
            }

            // Wait for the coroutine to reach the suspension point
            kotlinx.coroutines.delay(100)

            // Verify monitor state was updated
            assertEquals(DecisionID.DRAW_COUNT(mockPlayer), monitor.currentlyWaitingFor)

            // Now it's safe to provide the value since invoke() has been called
            SUT.provide(expectedResult)

            waitingJob.join()
            assertEquals(expectedResult, actualResult)
        }
    }
} 
