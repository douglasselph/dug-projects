package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.ChoiceDie
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.ui.support.DecisionID
import dugsolutions.leaf.player.decisions.ui.support.DecisionMonitor
import dugsolutions.leaf.random.die.SampleDie
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DecisionAcquireSelectSuspendTest {

    private lateinit var possibleCards: List<ChoiceCard>
    private lateinit var possibleDice: List<ChoiceDie>
    private val sampleDie = SampleDie()
    private val monitor = DecisionMonitor()
    private val SUT = DecisionAcquireSelectSuspend(monitor)

    @BeforeEach
    fun setup() {
        possibleCards = listOf(ChoiceCard(card = FakeCards.fakeCanopy, Combination()))
        possibleDice = listOf(ChoiceDie(die = sampleDie.d6, Combination()))
    }

    @Test
    fun invoke_whenWaitingForCard_returnsProvidedCard() = runBlocking {
        // Arrange
        val expectedResult = DecisionAcquireSelect.BuyItem.Card(possibleCards[0])
        var actualResult: DecisionAcquireSelect.BuyItem? = null
        
        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT(possibleCards, possibleDice)
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(100)

        // Verify monitor state was updated
        assertEquals(DecisionID.ACQUIRE_SELECT(possibleCards.map { it.card }, possibleDice.map { it.die }), 
                    monitor.currentlyWaitingFor)

        // Provide the value
        SUT.provide(possibleCards[0].card)

        // Wait for the waiting coroutine to complete
        waitingJob.join()

        // Assert
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun invoke_whenWaitingForDie_returnsProvidedDie() = runBlocking {
        // Arrange
        val expectedResult = DecisionAcquireSelect.BuyItem.Die(possibleDice[0])
        var actualResult: DecisionAcquireSelect.BuyItem? = null
        
        // Act - Start waiting in a separate coroutine
        val waitingJob = launch {
            actualResult = SUT(possibleCards, possibleDice)
        }

        // Wait for the coroutine to reach the suspension point
        kotlinx.coroutines.delay(100)

        // Verify monitor state was updated
        assertEquals(DecisionID.ACQUIRE_SELECT(possibleCards.map { it.card }, possibleDice.map { it.die }), 
                    monitor.currentlyWaitingFor)

        // Provide the value
        SUT.provide(possibleDice[0].die)

        // Wait for the waiting coroutine to complete
        waitingJob.join()

        // Assert
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun invoke_whenValueProvidedFirst_throwsException() = runBlocking {
        // Arrange
        val expectedException = "Called provide() before invoke() function."
        
        // Act & Assert
        val exception = org.junit.jupiter.api.assertThrows<Exception> {
            SUT.provide(possibleCards[0].card)
        }
        
        assertEquals(expectedException, exception.message)
    }

    @Test
    fun invoke_whenCalledMultipleTimes_returnsCorrectValuesInSequence() = runBlocking {
        // Arrange
        val expectedResults = listOf(
            DecisionAcquireSelect.BuyItem.Card(possibleCards[0]),
            DecisionAcquireSelect.BuyItem.Die(possibleDice[0])
        )

        // Act & Assert
        for (expectedResult in expectedResults) {
            var actualResult: DecisionAcquireSelect.BuyItem? = null
            val waitingJob = launch {
                actualResult = SUT(possibleCards, possibleDice)
            }

            // Wait for the coroutine to reach the suspension point
            kotlinx.coroutines.delay(100)

            // Verify monitor state was updated
            assertEquals(DecisionID.ACQUIRE_SELECT(possibleCards.map { it.card }, possibleDice.map { it.die }), 
                        monitor.currentlyWaitingFor)
            
            // Now it's safe to provide the value since invoke() has been called
            when (expectedResult) {
                is DecisionAcquireSelect.BuyItem.Card -> SUT.provide(possibleCards[0].card)
                is DecisionAcquireSelect.BuyItem.Die -> SUT.provide(possibleDice[0].die)
                DecisionAcquireSelect.BuyItem.None -> throw Exception("Did not expect the None result")
            }
            
            waitingJob.join()
            assertEquals(expectedResult, actualResult)
        }
    }
} 
