package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.FakeCombination
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AcquireACardEvaluatorTest {

    companion object {
        private val fakeCard1 = FakeCards.rootCard
        private val fakeCard2 = FakeCards.canopyCard
        private val fakeCard3 = FakeCards.flowerCard
        private val fakeCard4 = FakeCards.vineCard
        private val fakeCard5 = FakeCards.seedlingCard
    }
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val bestCardEvaluator = mockk<BestCardEvaluator>(relaxed = true)
    private val SUT = AcquireCardEvaluator(bestCardEvaluator)

    @Test
    fun invoke_whenBestCardExists_returnsMatchingChoice() {
        // Arrange
        val choice1 = ChoiceCard(fakeCard1, FakeCombination.combinationD6)
        val choice2 = ChoiceCard(fakeCard2, FakeCombination.combinationD8)
        val choice3 = ChoiceCard(fakeCard3, FakeCombination.combinationD10)

        every { bestCardEvaluator(mockPlayer, listOf(fakeCard1, fakeCard2, fakeCard3)) } returns fakeCard2

        // Act
        val result = SUT(mockPlayer, listOf(choice1, choice2, choice3))

        // Assert
        assertEquals(choice2, result)
    }

    @Test
    fun invoke_whenBestCardNotInChoices_returnsNull() {
        // Arrange
        val choice1 = ChoiceCard(fakeCard1, FakeCombination.combinationD6)
        val choice2 = ChoiceCard(fakeCard2, FakeCombination.combinationD8)
        every { bestCardEvaluator(mockPlayer, listOf(fakeCard1, fakeCard2)) } returns fakeCard3

        // Act
        val result = SUT(mockPlayer, listOf(choice1, choice2))

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenSingleChoice_returnsThatChoice() {
        // Arrange
        val choice = ChoiceCard(fakeCard1, FakeCombination.combinationD6)
        every { bestCardEvaluator(mockPlayer, listOf(fakeCard1)) } returns fakeCard1

        // Act
        val result = SUT(mockPlayer, listOf(choice))

        // Assert
        assertEquals(choice, result)
    }

    @Test
    fun invoke_whenMultipleChoicesForSameCard_returnsFirstChoice() {
        // Arrange
        val choice1 = ChoiceCard(fakeCard1, FakeCombination.combinationD6)
        val choice2 = ChoiceCard(fakeCard1, FakeCombination.combinationD8)
        every { bestCardEvaluator(mockPlayer, any()) } returns fakeCard1

        // Act
        val result = SUT(mockPlayer, listOf(choice1, choice2))

        // Assert
        assertEquals(choice1, result)
    }

    @Test
    fun invoke_whenBestCardIsLastInList_returnsCorrectChoice() {
        // Arrange
        val choice1 = ChoiceCard(fakeCard1, FakeCombination.combinationD6)
        val choice2 = ChoiceCard(fakeCard2, FakeCombination.combinationD8)
        val choice3 = ChoiceCard(fakeCard3, FakeCombination.combinationD10)
        val choice4 = ChoiceCard(fakeCard4, FakeCombination.combinationD12)
        val choice5 = ChoiceCard(fakeCard5, FakeCombination.combinationD4D6D8)

        every { bestCardEvaluator(mockPlayer, listOf(fakeCard1, fakeCard2, fakeCard3, fakeCard4, fakeCard5)) } returns fakeCard5

        // Act
        val result = SUT(mockPlayer, listOf(choice1, choice2, choice3, choice4, choice5))

        // Assert
        assertEquals(choice5, result)
    }

    @Test
    fun invoke_whenBestCardIsFirstInList_returnsCorrectChoice() {
        // Arrange
        val choice1 = ChoiceCard(fakeCard1, FakeCombination.combinationD6)
        val choice2 = ChoiceCard(fakeCard2, FakeCombination.combinationD8)
        val choice3 = ChoiceCard(fakeCard3, FakeCombination.combinationD10)
        val choice4 = ChoiceCard(fakeCard4, FakeCombination.combinationD12)
        val choice5 = ChoiceCard(fakeCard5, FakeCombination.combinationD6Plus5)

        every { bestCardEvaluator(mockPlayer, listOf(fakeCard1, fakeCard2, fakeCard3, fakeCard4, fakeCard5)) } returns fakeCard1

        // Act
        val result = SUT(mockPlayer, listOf(choice1, choice2, choice3, choice4, choice5))

        // Assert
        assertEquals(choice1, result)
    }

    @Test
    fun invoke_whenBestCardIsMiddleInList_returnsCorrectChoice() {
        // Arrange
        val choice1 = ChoiceCard(fakeCard1, FakeCombination.combinationD6)
        val choice2 = ChoiceCard(fakeCard2, FakeCombination.combinationD8)
        val choice3 = ChoiceCard(fakeCard3, FakeCombination.combinationD10)
        val choice4 = ChoiceCard(fakeCard4, FakeCombination.combinationD12)
        val choice5 = ChoiceCard(fakeCard5, FakeCombination.combinationD4D6D8)

        every { bestCardEvaluator(mockPlayer, listOf(fakeCard1, fakeCard2, fakeCard3, fakeCard4, fakeCard5)) } returns fakeCard3

        // Act
        val result = SUT(mockPlayer, listOf(choice1, choice2, choice3, choice4, choice5))

        // Assert
        assertEquals(choice3, result)
    }
} 
