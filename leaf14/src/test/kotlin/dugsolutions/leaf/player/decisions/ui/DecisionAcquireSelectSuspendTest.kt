package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.common.domain.acquire.ChoiceCard
import dugsolutions.leaf.common.domain.acquire.ChoiceDie
import dugsolutions.leaf.common.domain.acquire.UsingDice
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.random.die.SampleDie
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecisionAcquireSelectSuspendTest {

    private lateinit var possibleCards: List<ChoiceCard>
    private lateinit var possibleDice: List<ChoiceDie>
    private lateinit var SUT: DecisionAcquireSelectSuspend
    private val sampleDie = SampleDie()

    @BeforeEach
    fun setup() {
        possibleCards = listOf(ChoiceCard(card = FakeCards.strongRootCard, UsingDice()))
        possibleDice = listOf(ChoiceDie(die = sampleDie.d6, UsingDice()))
        SUT = DecisionAcquireSelectSuspend()
    }

    @Test
    fun invoke_whenWaitingForDecision_suspends() = runBlocking {
        // Arrange
        var result: DecisionAcquireSelect.BuyItem? = null
        var completed = false

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            result = SUT(possibleCards, possibleDice)
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
    fun invoke_whenCardDecisionProvided_returnsCard() = runBlocking {
        // Arrange
        val expectedResult = DecisionAcquireSelect.BuyItem.Card(possibleCards[0])
        var actualResult: DecisionAcquireSelect.BuyItem? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT(possibleCards, possibleDice)
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
    fun invoke_whenDieDecisionProvided_returnsDie() = runBlocking {
        // Arrange
        val expectedResult = DecisionAcquireSelect.BuyItem.Die(possibleDice[0])
        var actualResult: DecisionAcquireSelect.BuyItem? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT(possibleCards, possibleDice)
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
    fun invoke_whenNoneDecisionProvided_returnsNone() = runBlocking {
        // Arrange
        val expectedResult = DecisionAcquireSelect.BuyItem.None
        var actualResult: DecisionAcquireSelect.BuyItem? = null

        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT(possibleCards, possibleDice)
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
        val result = DecisionAcquireSelect.BuyItem.Card(possibleCards[0])

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
            SUT(possibleCards, possibleDice)
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
            SUT(possibleCards, possibleDice)
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
            DecisionAcquireSelect.BuyItem.Card(possibleCards[0]),
            DecisionAcquireSelect.BuyItem.Die(possibleDice[0]),
            DecisionAcquireSelect.BuyItem.None
        )
        val actualResults = mutableListOf<DecisionAcquireSelect.BuyItem>()

        // Act & Assert
        for (expectedResult in results) {
            val waitingJob = launch {
                actualResults.add(SUT(possibleCards, possibleDice))
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
