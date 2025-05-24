package dugsolutions.leaf.player.components

import dugsolutions.leaf.components.GameCard
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectTrackerTest {

    private lateinit var effectTracker: EffectTracker
    private lateinit var testCard: GameCard

    @BeforeEach
    fun setup() {
        effectTracker = EffectTracker()
        testCard = mockk()
    }

    @Test
    fun clear_resetsAllValuesToInitialState() {
        // Arrange
        effectTracker.incomingDamage = 5
        effectTracker.thornDamage = 3
        effectTracker.deflectDamage = 2
        effectTracker.pipModifier = 5
        effectTracker.cardsReused.add(testCard)

        // Act
        effectTracker.clear()

        // Assert
        assertEquals(0, effectTracker.incomingDamage)
        assertEquals(0, effectTracker.thornDamage)
        assertEquals(0, effectTracker.deflectDamage)
        assertEquals(0, effectTracker.pipModifier)
        assertTrue(effectTracker.cardsReused.isEmpty())
    }
} 
