package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.GameCard
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class DecisionIDBestBloomAcquisitionCardBaselineTest {

    private lateinit var SUT: DecisionBestBloomAcquisitionCardBaseline

    @BeforeEach
    fun setup() {
        SUT = DecisionBestBloomAcquisitionCardBaseline()
    }

    @Test
    fun invoke_whenEmptyList_throwsException() {
        // Arrange
        val emptyList = emptyList<GameCard>()

        // Act & Assert
        assertThrows<NoSuchElementException> {
            SUT(emptyList)
        }
    }

    @Test
    fun invoke_whenSingleCard_returnsThatCard() {
        // Arrange
        val bloom = FakeCards.fakeBloom
        val cards = listOf(bloom)

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(bloom, result)
    }

    @Test
    fun invoke_whenMultipleCards_returnsFirstCard() {
        // Arrange
        val bloom1 = FakeCards.fakeBloom
        val bloom2 = FakeCards.fakeBloom2
        val cards = listOf(bloom1, bloom2)

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(bloom1, result)
    }
} 
