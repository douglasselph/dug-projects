package dugsolutions.leaf.player.components

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.tool.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerStateTest {
    private lateinit var playerState: PlayerState
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var mockDie: Die

    @BeforeEach
    fun setup() {
        // Initialize random components
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)

        // Create a mock die
        mockDie = dieFactory(DieSides.D6)

        // Initialize player state
        playerState = PlayerState()
    }

    @Test
    fun isDormant_whenInitialized_returnsFalse() {
        // Act & Assert
        assertFalse(playerState.isDormant)
    }

    @Test
    fun isDormant_whenSetToTrue_returnsTrue() {
        // Act
        playerState.isDormant = true

        // Assert
        assertTrue(playerState.isDormant)
    }

    @Test
    fun bonusDie_whenInitialized_returnsNull() {
        // Act & Assert
        assertNull(playerState.bonusDie)
    }

    @Test
    fun bonusDie_whenSetToDie_returnsDie() {
        // Act
        playerState.bonusDie = mockDie

        // Assert
        assertEquals(mockDie, playerState.bonusDie)
    }

    @Test
    fun hasPassed_whenInitialized_returnsFalse() {
        // Act & Assert
        assertFalse(playerState.hasPassed)
    }

    @Test
    fun hasPassed_whenSetToTrue_returnsTrue() {
        // Act
        playerState.hasPassed = true

        // Assert
        assertTrue(playerState.hasPassed)
    }

    @Test
    fun clear_whenCalled_resetsAllProperties() {
        // Arrange
        playerState.isDormant = true
        playerState.bonusDie = mockDie
        playerState.hasPassed = true

        // Act
        playerState.clear()

        // Assert
        assertFalse(playerState.isDormant)
        assertNull(playerState.bonusDie)
        assertFalse(playerState.hasPassed)
    }
} 