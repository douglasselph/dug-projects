package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.GameCard
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class DecisionBestBloomCardCoreStrategyTest {

    private lateinit var SUT: DecisionBestBloomCardCoreStrategy

    @BeforeEach
    fun setup() {
        SUT = DecisionBestBloomCardCoreStrategy()
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
