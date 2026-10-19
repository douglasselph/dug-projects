package dugsolutions.leaf.v30.game.effect.scope

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HandleDieEffectScopeTest {

    @Test
    fun allDice_returnsCurrentPlayerHandDice() {
        val d6 = TestDie(6, 3)
        val d8 = TestDie(8, 5)
        val player = player(d6, d8)
        val scope = HandleDieEffectScope(player)

        val result = scope.allDice()

        assertEquals(2, result.size)
        assertTrue(result.hasDie(d6))
        assertTrue(result.hasDie(d8))
        assertEquals("player ${player.id}'s hand", scope.locationDescription)
        assertSame(player, scope.actingPlayer)
        assertSame(player, scope.targetPlayer)
    }

    @Test
    fun hasDieAndFindDie_whenDieExists_returnTrueAndDie() {
        val die = TestDie(6, 3)
        val player = player(die)
        val scope = HandleDieEffectScope(player)

        assertTrue(scope.hasDie(die))
        assertSame(die, scope.findDie(die))
    }

    @Test
    fun hasDieAndFindDie_whenDieDoesNotExist_returnFalseAndNull() {
        val player = player(TestDie(6, 3))
        val missing = TestDie(8, 3)
        val scope = HandleDieEffectScope(player)

        assertFalse(scope.hasDie(missing))
        assertNull(scope.findDie(missing))
        assertFalse(scope.hasDie(null))
        assertNull(scope.findDie(null))
    }

    @Test
    fun reroll_whenDieExists_updatesHandDie() {
        val die = TestDie(6, 2, rollValues = listOf(5))
        val player = player(die)
        val scope = HandleDieEffectScope(player)

        val result = scope.reroll(die)

        assertSame(die, result)
        assertEquals(5, die.value)
        assertEquals(5, player.diceHand.dice.single().value)
    }

    @Test
    fun raise_whenDieExists_adjustsHandDie() {
        val die = TestDie(6, 2)
        val player = player(die)
        val scope = HandleDieEffectScope(player)

        val result = scope.raise(die, 3)

        assertSame(die, result)
        assertEquals(5, die.value)
        assertEquals(5, player.diceHand.dice.single().value)
    }

    @Test
    fun setValue_whenDieExists_setsHandDieValue() {
        val die = TestDie(6, 2)
        val player = player(die)
        val scope = HandleDieEffectScope(player)

        val result = scope.setValue(die, 6)

        assertSame(die, result)
        assertEquals(6, die.value)
        assertEquals(6, player.diceHand.dice.single().value)
    }

    @Test
    fun mutationMethods_whenDieDoesNotExist_returnNull() {
        val player = player(TestDie(6, 2))
        val missing = TestDie(8, 2)
        val scope = HandleDieEffectScope(player)

        assertNull(scope.reroll(missing))
        assertNull(scope.raise(missing, 1))
        assertNull(scope.setValue(missing, 7))
    }

    private fun player(vararg dice: Die): Player {
        return Player().apply {
            dice.forEach { addDieToHand(it) }
        }
    }

    private class TestDie(
        sides: Int,
        value: Int,
        private val rollValues: List<Int> = emptyList()
    ) : Die(sides) {
        private var rollIndex = 0

        init {
            adjustTo(value)
        }

        override fun roll(): Die {
            if (rollValues.isNotEmpty()) {
                adjustTo(rollValues.getOrElse(rollIndex) { rollValues.last() })
                rollIndex++
            }
            return this
        }
    }
}
