package dugsolutions.leaf.v30.battle

import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BattleTest {

    @Test
    fun setup_ordersColumnsByPlayerHandDice() {
        val players = listOf(
            player(1, FixedDie(20, 4), FixedDie(6, 2), FixedDie(8, 1)),
            player(2, FixedDie(6, 6), FixedDie(4, 1), FixedDie(8, 1)),
            player(3, FixedDie(8, 5), FixedDie(6, 3), FixedDie(20, 1)),
            player(4, FixedDie(4, 2), FixedDie(6, 2), FixedDie(8, 2))
        )
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))

        battle.setup(players)

        assertEquals(listOf(2, 3, 1, 4), battle.grid.playerIds)
    }

    @Test
    fun setup_placesPlayerDiceIntoStrikeRowsByValueThenLowestSides() {
        val target = player(1, FixedDie(20, 3), FixedDie(6, 3), FixedDie(8, 6))
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer()))

        battle.setup(
            listOf(
                target,
                player(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                player(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                player(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )

        assertDie(sides = 8, value = 6, item = battle.grid.getSquare(1, BattleStrikeRow.STRIKE_1).all.single())
        assertDie(sides = 6, value = 3, item = battle.grid.getSquare(1, BattleStrikeRow.STRIKE_2).all.single())
        assertDie(sides = 20, value = 3, item = battle.grid.getSquare(1, BattleStrikeRow.STRIKE_3).all.single())
    }

    @Test
    fun setup_whenPlayerDoesNotHaveExactlyThreeDice_recordsWarning() {
        val chronicle = GameChronicle(currentRound = { 7 })
        val battle = Battle(chronicle = chronicle, playerGridOrder = PlayerGridOrder(SequentialRandomizer()))
        val target = player(1, FixedDie(6, 5), FixedDie(8, 4))

        battle.setup(
            listOf(
                target,
                player(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                player(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                player(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )

        val warning = assertIs<GameEntry.Warning>(chronicle.getEntries().single())
        assertEquals(WarningType.BATTLE_HAND_DICE_COUNT_NOT_THREE, warning.type)
        assertEquals(1, warning.playerId)
        assertEquals(7, warning.time.round)
        assertEquals(2, warning.actualCount)
    }

    private fun player(
        id: Int,
        vararg dice: Die
    ): Player {
        return Player(id = id).apply {
            dice.forEach { addDieToSupply(it) }
            repeat(dice.size) { drawDie() }
        }
    }

    private fun assertDie(
        sides: Int,
        value: Int,
        item: BattleItem
    ) {
        val dieItem = assertIs<BattleItem.DieItem>(item)
        assertEquals(sides, dieItem.die.sides)
        assertEquals(value, dieItem.die.value)
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
