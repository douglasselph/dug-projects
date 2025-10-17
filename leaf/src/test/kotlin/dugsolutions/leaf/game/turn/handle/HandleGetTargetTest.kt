package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.PlayerTD.Companion.randomizerTD
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HandleGetTargetTest {

    // Test subject
    private lateinit var SUT: HandleGetTarget

    // Test data
    private lateinit var mockPlayer1: Player
    private lateinit var mockPlayer2: Player
    private lateinit var mockPlayer3: Player
    private lateinit var mockPlayer4: Player
    private lateinit var allPlayers: List<Player>

    @BeforeEach
    fun setup() {
        // Create test players
        mockPlayer1 = mockk(relaxed = true) {
            every { id } returns 1
        }
        mockPlayer2 = mockk(relaxed = true) {
            every { id } returns 2
        }
        mockPlayer3 = mockk(relaxed = true) {
            every { id } returns 3
        }
        mockPlayer4 = mockk(relaxed = true) {
            every { id } returns 4
        }
        allPlayers = listOf(mockPlayer1, mockPlayer2, mockPlayer3, mockPlayer4)

        // Create the test subject
        SUT = HandleGetTarget()
    }

    @Test
    fun invoke_with4PlayerPlayers_returnsNextTargetAsExpected() {
        // Act
        val result1 = SUT(mockPlayer1, allPlayers)
        val result2 = SUT(mockPlayer2, allPlayers)
        val result3 = SUT(mockPlayer3, allPlayers)
        val result4 = SUT(mockPlayer4, allPlayers)

        // Assert
        assertEquals(mockPlayer2, result1, "Player 1's target should be Player 2")
        assertEquals(mockPlayer3, result2, "Player 2's target should be Player 3")
        assertEquals(mockPlayer4, result3, "Player 3's target should be Player 4")
        assertEquals(mockPlayer1, result4, "Player 4's target should be Player 1")

    }

    @Test
    fun invoke_withNoValidTargets_returnsNull() {
        // Arrange
        val allTargetsHit = listOf(
            mockPlayer1
        )

        // Act
        assertThrows<Exception> {
            SUT(mockPlayer1, allTargetsHit)
        }
    }

} 
