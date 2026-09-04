package dugsolutions.leaf.v35.player.critter

import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CritterValueStateTest {

    @Test
    fun valueOf_defaultsToPhysicalCritterBaseValue() {
        val state = CritterValueState()

        assertEquals(1, state.valueOf(Critter.WORM))
        assertEquals(2, state.valueOf(Critter.BEE))
    }

    @Test
    fun boostForRound_addsToCurrentValue() {
        val state = CritterValueState()

        state.boostForRound(Critter.WORM, 2)

        assertEquals(3, state.valueOf(Critter.WORM))
        assertEquals(2, state.valueOf(Critter.BEE))
        assertTrue(state.hasRoundOverride(Critter.WORM))
        assertFalse(state.hasRoundOverride(Critter.BEE))
    }

    @Test
    fun boostForRound_stacks() {
        val state = CritterValueState()

        state.boostForRound(Critter.WORM, 2)
        state.boostForRound(Critter.WORM, 2)

        assertEquals(5, state.valueOf(Critter.WORM))
    }

    @Test
    fun setForRound_establishesExactValue() {
        val state = CritterValueState()

        state.setForRound(Critter.BEE, 4)
        state.setForRound(Critter.BEE, 4)

        assertEquals(4, state.valueOf(Critter.BEE))
        assertEquals(1, state.valueOf(Critter.WORM))
    }

    @Test
    fun clearRound_restoresBaseValues() {
        val state = CritterValueState()
        state.boostForRound(Critter.WORM, 4)
        state.setForRound(Critter.BEE, 4)

        state.clearRound()

        assertEquals(1, state.valueOf(Critter.WORM))
        assertEquals(2, state.valueOf(Critter.BEE))
        assertTrue(state.overrides.isEmpty())
    }

    @Test
    fun boostForRound_requiresPositiveAmount() {
        val state = CritterValueState()

        assertFailsWith<IllegalArgumentException> {
            state.boostForRound(Critter.WORM, 0)
        }
    }

    @Test
    fun setForRound_requiresPositiveValue() {
        val state = CritterValueState()

        assertFailsWith<IllegalArgumentException> {
            state.setForRound(Critter.WORM, 0)
        }
    }

    @Test
    fun overrides_isDefensiveSnapshot() {
        val state = CritterValueState()
        state.boostForRound(Critter.WORM, 2)
        val snapshot = state.overrides

        state.clearRound()

        assertEquals(mapOf(Critter.WORM to 3), snapshot)
        assertTrue(state.overrides.isEmpty())
    }
}
