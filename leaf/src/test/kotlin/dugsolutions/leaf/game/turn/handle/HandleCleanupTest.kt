package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleCleanupTest {

    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockHandleReused: HandleReused = mockk(relaxed = true)
    private val mockHandleRetained: HandleRetained = mockk(relaxed = true)

    private val SUT: HandleCleanup = HandleCleanup(mockHandleReused, mockHandleRetained)

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_verifiesExactOrderOfOperations() = runBlocking {
        // Arrange
        val sequence = mutableListOf<String>()
        every { mockPlayer.discardHand() } answers { sequence.add("discardHand") }
        coEvery { mockPlayer.drawHand() } answers { sequence.add("drawHand") }
        every { mockHandleReused(mockPlayer) } answers { sequence.add("handleReused") }
        every { mockHandleRetained(mockPlayer) } answers { sequence.add("handleRetained") }

        // Act
        SUT(mockPlayer)

        // Assert
        val expectedSequence = listOf("discardHand", "drawHand", "handleReused", "handleRetained")
        assertEquals(expectedSequence, sequence) { "Expected sequence $expectedSequence but got $sequence" }
    }

} 
