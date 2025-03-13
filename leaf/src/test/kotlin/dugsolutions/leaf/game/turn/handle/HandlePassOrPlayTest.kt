package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandlePassOrPlayTest {

    companion object {
        private const val PLAYER_ID = 1
        private const val PLAYER_NAME = "Test Player"
    }

    private lateinit var SUT: HandlePassOrPlay
    private lateinit var mockPlayer: Player

    @BeforeEach
    fun setup() {
        SUT = HandlePassOrPlay()
        mockPlayer = mockk(relaxed = true)
        
        // Setup basic player properties
        every { mockPlayer.id } returns PLAYER_ID
        every { mockPlayer.name } returns PLAYER_NAME
    }

    @Test
    fun invoke_whenPlayerCannotPlayCard_returnsFalse() {
        // Arrange
        every { mockPlayer.canPlayCard } returns false

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertFalse(result)
        verify { mockPlayer.canPlayCard }
        verify(exactly = 0) { mockPlayer.cardsInHand }
        verify(exactly = 0) { mockPlayer.hasPassed }
    }

    @Test
    fun invoke_whenPlayerHasNoCards_returnsFalseAndSetsHasPassed() {
        // Arrange
        every { mockPlayer.canPlayCard } returns true
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertFalse(result)
        verify { mockPlayer.canPlayCard }
        verify { mockPlayer.cardsToPlay }
        verify { mockPlayer.hasPassed = true }
    }

    @Test
    fun invoke_whenPlayerHasCards_returnsTrue() {
        // Arrange
        every { mockPlayer.canPlayCard } returns true
        every { mockPlayer.cardsToPlay } returns mutableListOf(mockk())

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertTrue(result)
        verify { mockPlayer.canPlayCard }
        verify { mockPlayer.cardsToPlay }
        verify(exactly = 0) { mockPlayer.hasPassed }
    }
} 
