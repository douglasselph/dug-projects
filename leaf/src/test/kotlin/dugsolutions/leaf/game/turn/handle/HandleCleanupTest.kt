package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleCleanupTest {

    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockHandleReused: HandleReused = mockk(relaxed = true)
    private val mockHandleRetained: HandleRetained = mockk(relaxed = true)
    private val mockCompostRecovery: HandleCompostRecovery = mockk(relaxed = true)

    private val SUT: HandleCleanup = HandleCleanup(
        mockHandleReused,
        mockHandleRetained,
        mockCompostRecovery
    )

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
        every { mockCompostRecovery(mockPlayer) } answers { sequence.add("handleCompostRecovery") }

        // Act
        SUT(mockPlayer)

        // Assert
        val expectedSequence = listOf("discardHand", "drawHand", "handleReused", "handleRetained", "handleCompostRecovery")
        assertEquals(expectedSequence, sequence) { "Expected sequence $expectedSequence but got $sequence" }
    }

    @Test
    fun invoke_whenCompostRecoveryCalled_processesCompostRecovery() = runBlocking {
        // Arrange
        // No special setup needed - just verify the call

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockCompostRecovery(mockPlayer) }
    }

} 
