package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitorReport
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DecisionShouldProcessTrashEffectSuspendTest {

    private val monitor = DecisionMonitor()
    private val mockDecisionMonitorReport = mockk<DecisionMonitorReport>(relaxed = true)
    private val SUT = DecisionShouldProcessTrashEffectSuspend(monitor, mockDecisionMonitorReport)

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_whenWaitingForDecision_returnsProvidedValue() = runBlocking {
        // Arrange
        val card = FakeCards.seedlingCard
        val expectedResult = DecisionShouldProcessTrashEffect.Result.TRASH
        var actualResult: DecisionShouldProcessTrashEffect.Result? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT(card)
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(100)

        // Verify monitor state was updated
        assertEquals(DecisionID.SHOULD_PROCESS_TRASH_EFFECT(card), monitor.currentlyWaitingFor)

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
        val card = FakeCards.vineCard
        val expectedResults = listOf(
            DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH,
            DecisionShouldProcessTrashEffect.Result.TRASH,
            DecisionShouldProcessTrashEffect.Result.TRASH_IF_NEEDED
        )

        // Act & Assert
        for (expectedResult in expectedResults) {
            var actualResult: DecisionShouldProcessTrashEffect.Result? = null
            val waitingJob = launch {
                actualResult = SUT(card)
            }

            // Wait for the coroutine to reach the suspension point
            kotlinx.coroutines.delay(100)

            // Verify monitor state was updated
            assertEquals(DecisionID.SHOULD_PROCESS_TRASH_EFFECT(card), monitor.currentlyWaitingFor)

            // Now it's safe to provide the value since invoke() has been called
            SUT.provide(expectedResult)

            waitingJob.join()
            assertEquals(expectedResult, actualResult)
        }
    }

    @Test
    fun invoke_withDifferentCards_returnsCorrectValuesForEachCard() = runBlocking {
        // Arrange
        val card1 = FakeCards.seedlingCard
        val card2 = FakeCards.vineCard
        val expectedResult1 = DecisionShouldProcessTrashEffect.Result.TRASH
        val expectedResult2 = DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH

        // Act & Assert for first card
        var actualResult1: DecisionShouldProcessTrashEffect.Result? = null
        val waitingJob1 = launch {
            actualResult1 = SUT(card1)
        }

        kotlinx.coroutines.delay(100)
        assertEquals(DecisionID.SHOULD_PROCESS_TRASH_EFFECT(card1), monitor.currentlyWaitingFor)
        SUT.provide(expectedResult1)
        waitingJob1.join()
        assertEquals(expectedResult1, actualResult1)

        // Act & Assert for second card
        var actualResult2: DecisionShouldProcessTrashEffect.Result? = null
        val waitingJob2 = launch {
            actualResult2 = SUT(card2)
        }

        kotlinx.coroutines.delay(100)
        assertEquals(DecisionID.SHOULD_PROCESS_TRASH_EFFECT(card2), monitor.currentlyWaitingFor)
        SUT.provide(expectedResult2)
        waitingJob2.join()
        assertEquals(expectedResult2, actualResult2)
    }

    @Test
    fun reset_doesNothing() {
        // Arrange & Act
        SUT.reset()

        // Assert - No exception should be thrown
        // This test verifies that reset() can be called without issues
    }
} 
