package dugsolutions.leaf.v30.game.effect.scope

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.PlayerGridOrder
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BattleDieEffectScopeTest {

    @Test
    fun allDice_returnsDiceFromTargetBattleSquare() {
        val targetDie = TestDie(6, 6)
        val target = player(1, targetDie, TestDie(8, 3), TestDie(10, 1))
        val acting = player(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
        val battle = setupBattle(target, acting)
        val scope = BattleDieEffectScope(battle, acting, target, BattleStrikeRow.STRIKE_1)

        val result = scope.allDice()

        assertEquals(1, result.size)
        assertTrue(result.hasDie(targetDie))
        assertEquals("player ${target.id}'s ${BattleStrikeRow.STRIKE_1} battle square", scope.locationDescription)
        assertSame(acting, scope.actingPlayer)
        assertSame(target, scope.targetPlayer)
    }

    @Test
    fun hasDieAndFindDie_whenDieExistsInSquare_returnTrueAndDie() {
        val targetDie = TestDie(6, 6)
        val target = player(1, targetDie, TestDie(8, 3), TestDie(10, 1))
        val acting = player(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
        val battle = setupBattle(target, acting)
        val scope = BattleDieEffectScope(battle, acting, target, BattleStrikeRow.STRIKE_1)

        assertTrue(scope.hasDie(targetDie))
        assertSame(targetDie, scope.findDie(targetDie))
    }

    @Test
    fun hasDieAndFindDie_whenDieIsInDifferentSquare_returnFalseAndNull() {
        val differentRowDie = TestDie(8, 3)
        val target = player(1, TestDie(6, 6), differentRowDie, TestDie(10, 1))
        val acting = player(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
        val battle = setupBattle(target, acting)
        val scope = BattleDieEffectScope(battle, acting, target, BattleStrikeRow.STRIKE_1)

        assertFalse(scope.hasDie(differentRowDie))
        assertNull(scope.findDie(differentRowDie))
        assertFalse(scope.hasDie(null))
        assertNull(scope.findDie(null))
    }

    @Test
    fun reroll_whenDieExists_updatesBattleGridDie() {
        val targetDie = TestDie(6, 6, rollValues = listOf(2))
        val target = player(1, targetDie, TestDie(8, 3), TestDie(10, 1))
        val acting = player(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
        val battle = setupBattle(target, acting)
        val scope = BattleDieEffectScope(battle, acting, target, BattleStrikeRow.STRIKE_1)

        val result = scope.reroll(targetDie)

        assertSame(targetDie, result)
        assertEquals(2, targetDie.value)
        assertEquals(2, scope.findDie(targetDie)?.value)
    }

    @Test
    fun raise_whenDieExists_adjustsBattleGridDie() {
        val targetDie = TestDie(6, 3)
        val target = player(1, targetDie, TestDie(8, 2), TestDie(10, 1))
        val acting = player(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
        val battle = setupBattle(target, acting)
        val scope = BattleDieEffectScope(battle, acting, target, BattleStrikeRow.STRIKE_1)

        val result = scope.raise(targetDie, 2)

        assertSame(targetDie, result)
        assertEquals(5, targetDie.value)
        assertEquals(5, scope.findDie(targetDie)?.value)
    }

    @Test
    fun setValue_whenDieExists_setsBattleGridDieValue() {
        val targetDie = TestDie(6, 3)
        val target = player(1, targetDie, TestDie(8, 2), TestDie(10, 1))
        val acting = player(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
        val battle = setupBattle(target, acting)
        val scope = BattleDieEffectScope(battle, acting, target, BattleStrikeRow.STRIKE_1)

        val result = scope.setValue(targetDie, 6)

        assertSame(targetDie, result)
        assertEquals(6, targetDie.value)
        assertEquals(6, scope.findDie(targetDie)?.value)
    }

    @Test
    fun mutationMethods_whenDieDoesNotExistInSquare_returnNull() {
        val target = player(1, TestDie(6, 3), TestDie(8, 2), TestDie(10, 1))
        val acting = player(2, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
        val battle = setupBattle(target, acting)
        val missing = TestDie(12, 4)
        val scope = BattleDieEffectScope(battle, acting, target, BattleStrikeRow.STRIKE_1)

        assertNull(scope.reroll(missing))
        assertNull(scope.raise(missing, 1))
        assertNull(scope.setValue(missing, 7))
    }

    private fun setupBattle(
        target: Player,
        acting: Player
    ): Battle {
        return Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer())).apply {
            setup(
                listOf(
                    target,
                    acting,
                    player(3, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1)),
                    player(4, TestDie(4, 1), TestDie(6, 1), TestDie(8, 1))
                )
            )
        }
    }

    private fun player(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
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

    private class SequentialRandomizer : Randomizer {
        private var next = 1

        override fun nextBoolean(): Boolean = true

        override fun nextInt(from: Int, until: Int): Int {
            val result = next.coerceIn(from, until - 1)
            next++
            return result
        }

        override fun nextInt(until: Int): Int = nextInt(0, until)
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
