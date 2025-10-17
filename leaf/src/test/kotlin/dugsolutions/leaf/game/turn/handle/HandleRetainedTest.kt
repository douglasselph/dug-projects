package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class HandleRetainedTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
    }

    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockCard1: GameCard = mockk(relaxed = true) {
        every { id } returns CARD_ID_1
        every { type } returns FlourishType.ROOT
    }
    private val mockCard2: GameCard = mockk(relaxed = true) {
        every { id } returns CARD_ID_2
        every { type } returns FlourishType.BLOOM
    }
    private val sampleDie = SampleDie()
    private val d6 = sampleDie.d6
    private val d8 = sampleDie.d8

    private val SUT: HandleRetained = HandleRetained()

    @BeforeEach
    fun setup() {
        every { mockPlayer.retained } returns mutableListOf()
    }

    @Test
    fun invoke_whenRetainedHasCards_addsCardsToHandAndClearsList() {
        // Arrange
        val retained = mutableListOf<HandItem>(
            HandItem.aCard(mockCard1),
            HandItem.aCard(mockCard2)
        )
        every { mockPlayer.retained } returns retained

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addCardsToHand(listOf(CARD_ID_1, CARD_ID_2)) }
        assertTrue(retained.isEmpty())
    }

    @Test
    fun invoke_whenRetainedHasDice_addsDiceToHandAndClearsList() {
        // Arrange
        val retained = mutableListOf<HandItem>(
            HandItem.aDie(d6),
            HandItem.aDie(d8)
        )
        every { mockPlayer.retained } returns retained

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDiceToHand(listOf(d6, d8)) }
        assertTrue(retained.isEmpty())
    }

    @Test
    fun invoke_whenRetainedHasMixedItems_addsAllToHandAndClearsList() {
        // Arrange
        val retained = mutableListOf(
            HandItem.aCard(mockCard1),
            HandItem.aDie(d6),
            HandItem.aCard(mockCard2),
            HandItem.aDie(d8)
        )
        every { mockPlayer.retained } returns retained

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addCardsToHand(listOf(CARD_ID_1, CARD_ID_2)) }
        verify { mockPlayer.addDiceToHand(listOf(d6, d8)) }
        assertTrue(retained.isEmpty())
    }
} 
