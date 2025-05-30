package dugsolutions.leaf.chronicle

import dugsolutions.leaf.chronicle.domain.ChronicleEntry
import dugsolutions.leaf.chronicle.domain.TransformMomentToEntry
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameChronicleTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
        private const val CARD_ID_3 = 3
    }

    private val mockGameTurn = mockk<GameTurn>(relaxed = true)
    private val mockTransformMomentToEntry = mockk<TransformMomentToEntry>(relaxed = true)
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val mockEntry = mockk<ChronicleEntry>(relaxed = true)
    private val SUT = GameChronicle(mockGameTurn, mockTransformMomentToEntry)

    @BeforeEach
    fun setup() {
        every { mockTransformMomentToEntry(any(), any()) } returns mockEntry
    }

    @Test
    fun getEntries_whenNoEntries_returnsEmptyList() {
        // Act
        val result = SUT.getEntries()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun getEntries_whenHasEntries_returnsAllEntries() {
        // Arrange
        val moment1 = GameChronicle.Moment.DRAW_CARD(mockPlayer, CARD_ID_1)
        val moment2 = GameChronicle.Moment.DRAW_CARD(mockPlayer, CARD_ID_2)
        SUT(moment1)
        SUT(moment2)

        // Act
        val result = SUT.getEntries()

        // Assert
        assertEquals(2, result.size)
        assertEquals(mockEntry, result[0])
        assertEquals(mockEntry, result[1])
    }

    @Test
    fun getNewEntries_whenFirstCall_returnsAllEntries() {
        // Arrange
        val moment1 = GameChronicle.Moment.DRAW_CARD(mockPlayer, CARD_ID_1)
        val moment2 = GameChronicle.Moment.DRAW_CARD(mockPlayer, CARD_ID_2)
        SUT(moment1)
        SUT(moment2)

        // Act
        val result = SUT.getNewEntries()

        // Assert
        assertEquals(2, result.size)
        assertEquals(mockEntry, result[0])
        assertEquals(mockEntry, result[1])
    }

    @Test
    fun getNewEntries_whenCalledMultipleTimes_returnsOnlyNewEntries() {
        // Arrange
        val moment1 = GameChronicle.Moment.DRAW_CARD(mockPlayer, CARD_ID_1)
        val moment2 = GameChronicle.Moment.DRAW_CARD(mockPlayer, CARD_ID_2)
        val moment3 = GameChronicle.Moment.DRAW_CARD(mockPlayer, CARD_ID_3)
        
        SUT(moment1)
        SUT(moment2)
        SUT.getNewEntries() // First call returns all entries
        
        // Act
        SUT(moment3)
        val result = SUT.getNewEntries() // Second call should return only the new entry

        // Assert
        assertEquals(1, result.size)
        assertEquals(mockEntry, result[0])
    }

    @Test
    fun clear_whenCalled_removesAllEntries() {
        // Arrange
        val moment1 = GameChronicle.Moment.DRAW_CARD(mockPlayer, CARD_ID_1)
        val moment2 = GameChronicle.Moment.DRAW_CARD(mockPlayer, CARD_ID_2)
        SUT(moment1)
        SUT(moment2)

        // Act
        SUT.clear()

        // Assert
        assertTrue(SUT.getEntries().isEmpty())
        assertTrue(SUT.getNewEntries().isEmpty())
    }

} 
