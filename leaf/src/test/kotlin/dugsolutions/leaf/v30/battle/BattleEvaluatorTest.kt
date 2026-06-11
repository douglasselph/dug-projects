package dugsolutions.leaf.v30.battle

import dugsolutions.leaf.v30.battle.domain.BattleGrid
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BattleEvaluatorTest {

    private val SUT = BattleEvaluator()

    @Test
    fun invoke_whenSingleHighestScore_returnsSingleWinner() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        grid.add(1, BattleStrikeRow.STRIKE_1, dieItem(6, 6))
        grid.add(2, BattleStrikeRow.STRIKE_1, dieItem(6, 4))
        grid.add(3, BattleStrikeRow.STRIKE_1, dieItem(6, 3))
        grid.add(4, BattleStrikeRow.STRIKE_1, dieItem(6, 1))

        val result = SUT(grid.snapshot())

        assertEquals(listOf(1), result[BattleStrikeRow.STRIKE_1].winners)
    }

    @Test
    fun invoke_whenHighestScoresTie_returnsMultipleWinners() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        grid.add(1, BattleStrikeRow.STRIKE_1, dieItem(6, 5))
        grid.add(2, BattleStrikeRow.STRIKE_1, dieItem(8, 5))
        grid.add(3, BattleStrikeRow.STRIKE_1, dieItem(6, 2))
        grid.add(4, BattleStrikeRow.STRIKE_1, dieItem(6, 1))

        val result = SUT(grid.snapshot())

        assertEquals(listOf(1, 2), result[BattleStrikeRow.STRIKE_1].winners)
    }

    @Test
    fun invoke_whenAllScoresTie_returnsNoWinners() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        listOf(1, 2, 3, 4).forEach { playerId ->
            grid.add(playerId, BattleStrikeRow.STRIKE_1, dieItem(6, 3))
        }

        val result = SUT(grid.snapshot())

        assertEquals(emptyList(), result[BattleStrikeRow.STRIKE_1].winners)
        assertEquals(emptyList(), result[BattleStrikeRow.STRIKE_1].wounded)
    }

    @Test
    fun invoke_whenLoserTrailsByFiveOrMore_returnsWoundedPlayers() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        grid.add(1, BattleStrikeRow.STRIKE_1, dieItem(10, 9))
        grid.add(2, BattleStrikeRow.STRIKE_1, dieItem(8, 4))
        grid.add(3, BattleStrikeRow.STRIKE_1, dieItem(6, 3))
        grid.add(4, BattleStrikeRow.STRIKE_1, dieItem(6, 5))

        val result = SUT(grid.snapshot())

        assertEquals(listOf(1), result[BattleStrikeRow.STRIKE_1].winners)
        assertEquals(listOf(2, 3), result[BattleStrikeRow.STRIKE_1].wounded)
    }

    @Test
    fun invoke_scoresDiceAndCrittersTogether() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        grid.add(1, BattleStrikeRow.STRIKE_1, dieItem(6, 3))
        grid.add(1, BattleStrikeRow.STRIKE_1, BattleItem.CritterItem(Critter.BEE))
        grid.add(2, BattleStrikeRow.STRIKE_1, dieItem(6, 4))
        grid.add(3, BattleStrikeRow.STRIKE_1, dieItem(6, 1))
        grid.add(4, BattleStrikeRow.STRIKE_1, dieItem(6, 1))

        val result = SUT(grid.snapshot())

        assertEquals(listOf(1), result[BattleStrikeRow.STRIKE_1].winners)
    }

    @Test
    fun invoke_whenSquareHasBulwarkToken_ignoresSquare() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        grid.add(1, BattleStrikeRow.STRIKE_1, BattleItem.BulwarkToken)
        grid.add(1, BattleStrikeRow.STRIKE_1, dieItem(20, 20))
        grid.add(2, BattleStrikeRow.STRIKE_1, dieItem(6, 4))
        grid.add(3, BattleStrikeRow.STRIKE_1, dieItem(6, 3))
        grid.add(4, BattleStrikeRow.STRIKE_1, dieItem(6, 1))

        val result = SUT(grid.snapshot())

        assertEquals(listOf(2), result[BattleStrikeRow.STRIKE_1].winners)
        assertEquals(emptyList(), result[BattleStrikeRow.STRIKE_1].wounded)
    }

    @Test
    fun invoke_whenAllRemainingScoresTieAfterBulwarkFilter_returnsNoWinners() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        grid.add(1, BattleStrikeRow.STRIKE_1, BattleItem.BulwarkToken)
        grid.add(2, BattleStrikeRow.STRIKE_1, dieItem(6, 3))
        grid.add(3, BattleStrikeRow.STRIKE_1, dieItem(8, 3))
        grid.add(4, BattleStrikeRow.STRIKE_1, dieItem(10, 3))

        val result = SUT(grid.snapshot())

        assertEquals(emptyList(), result[BattleStrikeRow.STRIKE_1].winners)
        assertEquals(emptyList(), result[BattleStrikeRow.STRIKE_1].wounded)
    }

    @Test
    fun invoke_returnsResultForAllStrikeRows() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))

        val result = SUT(grid.snapshot())

        assertEquals(BattleStrikeRow.entries.toSet(), result.rows.keys)
    }

    private fun dieItem(sides: Int, value: Int): BattleItem.DieItem {
        return BattleItem.DieItem(FixedDie(sides, value))
    }

    private class FixedDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }
}
