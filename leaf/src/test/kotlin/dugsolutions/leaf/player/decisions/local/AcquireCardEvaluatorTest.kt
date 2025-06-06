package dugsolutions.leaf.player.decisions.local


import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.FakeCombination
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AcquireCardEvaluatorTest {

    companion object {
        private val fakeCard1 = FakeCards.fakeRoot
        private val fakeCard2 = FakeCards.fakeCanopy
        private val fakeCard3 = FakeCards.fakeFlower
    }
    private val bestCardEvaluator = mockk<BestCardEvaluator>(relaxed = true)

    private val SUT = AcquireCardEvaluator(bestCardEvaluator)

    @Test
    fun invoke_whenBestCardExists_returnsMatchingChoice() {
        // Arrange
        val choice1 = ChoiceCard(fakeCard1, FakeCombination.combinationD6)
        val choice2 = ChoiceCard(fakeCard2, FakeCombination.combinationD8)
        val choice3 = ChoiceCard(fakeCard3, FakeCombination.combinationD10)

        every { bestCardEvaluator(listOf(fakeCard1, fakeCard2, fakeCard3)) } returns fakeCard2

        // Act
        val result = SUT(listOf(choice1, choice2, choice3))

        // Assert
        assertEquals(choice2, result)
    }

    @Test
    fun invoke_whenBestCardNotInChoices_returnsNull() {
        // Arrange
        val choice1 = ChoiceCard(fakeCard1, FakeCombination.combinationD6)
        val choice2 = ChoiceCard(fakeCard2, FakeCombination.combinationD8)
        every { bestCardEvaluator(listOf(fakeCard1, fakeCard2)) } returns fakeCard3

        // Act
        val result = SUT(listOf(choice1, choice2))

        // Assert
        assertNull(result)
    }

} 
