package dugsolutions.leaf.v30.battle

import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.GameEntry
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Critter
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

        assertEquals(listOf(2, 3, 1, 4), battle.grid.playerIdsInGridOrder)
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

    @Test
    fun add_withDie_placesDieInPlayersSelectedSquare() {
        val target = player(1, FixedDie(6, 3), FixedDie(8, 2), FixedDie(10, 1))
        val die = FixedDie(12, 5)
        val battle = setupBattle(target)

        val result = battle.add(target, BattleStrikeRow.STRIKE_1, die)

        assertEquals(true, result)
        val items = battle.grid.getSquare(1, BattleStrikeRow.STRIKE_1).all
        assertDie(sides = 6, value = 3, item = items[0])
        assertDie(sides = 12, value = 5, item = items[1])
    }

    @Test
    fun add_withCritter_placesCritterInPlayersSelectedSquare() {
        val target = player(1, FixedDie(6, 3), FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)

        val result = battle.add(target, BattleStrikeRow.STRIKE_2, Critter.BEE)

        assertEquals(true, result)
        assertEquals(
            BattleItem.CritterItem(Critter.BEE),
            battle.grid.getSquare(1, BattleStrikeRow.STRIKE_2).all[1]
        )
    }

    @Test
    fun addBulwarkToken_placesBulwarkTokenInPlayersSelectedSquare() {
        val target = player(1, FixedDie(6, 3), FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)

        val result = battle.addBulwarkToken(target, BattleStrikeRow.STRIKE_2)

        assertEquals(true, result)
        assertEquals(
            BattleItem.BulwarkToken,
            battle.grid.getSquare(1, BattleStrikeRow.STRIKE_2).all[1]
        )
    }

    @Test
    fun add_whenSelectedSquareAlreadyHasThreeItems_returnsFalseAndDoesNotAdd() {
        val target = player(1, FixedDie(6, 3), FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)
        battle.add(target, BattleStrikeRow.STRIKE_1, Critter.BEE)
        battle.add(target, BattleStrikeRow.STRIKE_1, Critter.WORM)

        val result = battle.add(target, BattleStrikeRow.STRIKE_1, FixedDie(12, 5))

        assertEquals(false, result)
        assertEquals(3, battle.grid.getSquare(1, BattleStrikeRow.STRIKE_1).size)
    }

    @Test
    fun addBulwarkToken_whenSelectedSquareAlreadyHasThreeDiceOrCritters_returnsTrueAndAdds() {
        val target = player(1, FixedDie(6, 3), FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)
        battle.add(target, BattleStrikeRow.STRIKE_1, Critter.BEE)
        battle.add(target, BattleStrikeRow.STRIKE_1, Critter.WORM)

        val result = battle.addBulwarkToken(target, BattleStrikeRow.STRIKE_1)

        assertEquals(true, result)
        assertEquals(4, battle.grid.getSquare(1, BattleStrikeRow.STRIKE_1).size)
        assertEquals(BattleItem.BulwarkToken, battle.grid.getSquare(1, BattleStrikeRow.STRIKE_1).all[3])
    }

    @Test
    fun setDieValue_whenDieExistsInSelectedSquare_updatesDieValue() {
        val die = FixedDie(6, 3)
        val target = player(1, die, FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)

        val result = battle.setDieValue(target, BattleStrikeRow.STRIKE_1, die, 5)

        assertEquals(true, result)
        assertEquals(5, die.value)
        assertDie(sides = 6, value = 5, item = battle.grid.getSquare(1, BattleStrikeRow.STRIKE_1).all.single())
    }

    @Test
    fun setDieValue_whenDieIsNotInSelectedSquare_returnsFalse() {
        val die = FixedDie(6, 3)
        val target = player(1, die, FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)

        val result = battle.setDieValue(target, BattleStrikeRow.STRIKE_2, die, 5)

        assertEquals(false, result)
        assertEquals(3, die.value)
    }

    @Test
    fun setDieValue_whenDieBelongsToDifferentPlayer_returnsFalse() {
        val die = FixedDie(6, 3)
        val target = player(1, die, FixedDie(8, 2), FixedDie(10, 1))
        val other = player(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
        val battle = Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer())).apply {
            setup(
                listOf(
                    target,
                    other,
                    player(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                    player(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
                )
            )
        }

        val result = battle.setDieValue(other, BattleStrikeRow.STRIKE_1, die, 5)

        assertEquals(false, result)
        assertEquals(3, die.value)
    }

    @Test
    fun raiseDie_whenDieExistsInSelectedSquare_raisesDieValueAndReturnsDie() {
        val die = FixedDie(6, 5)
        val target = player(1, die, FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)

        val result = battle.raiseDie(target, BattleStrikeRow.STRIKE_1, FixedDie(6, 5), 3)

        assertEquals(die, result)
        assertEquals(6, die.value)
        assertDie(sides = 6, value = 6, item = battle.grid.getSquare(1, BattleStrikeRow.STRIKE_1).all.single())
    }

    @Test
    fun raiseDie_whenDieIsNotInSelectedSquare_returnsNull() {
        val die = FixedDie(6, 3)
        val target = player(1, die, FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)

        val result = battle.raiseDie(target, BattleStrikeRow.STRIKE_2, FixedDie(6, 3), 1)

        assertEquals(null, result)
        assertEquals(3, die.value)
    }

    @Test
    fun computeWinners_returnsBattleEvaluatorResultForCurrentGrid() {
        val target = player(1, FixedDie(6, 6), FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)

        val result = battle.computeWinners()

        assertEquals(listOf(1), result[BattleStrikeRow.STRIKE_1].winners)
    }

    @Test
    fun computeWinners_whenRowIsResolved_ignoresResolvedRow() {
        val target = player(1, FixedDie(6, 6), FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)

        battle.resolved(BattleStrikeRow.STRIKE_1)
        val result = battle.computeWinners()

        assertEquals(setOf(BattleStrikeRow.STRIKE_1), battle.resolved)
        assertEquals(emptyList(), result[BattleStrikeRow.STRIKE_1].winners)
    }

    @Test
    fun setup_clearsResolvedRows() {
        val target = player(1, FixedDie(6, 6), FixedDie(8, 2), FixedDie(10, 1))
        val battle = setupBattle(target)
        battle.resolved(BattleStrikeRow.STRIKE_1)

        battle.setup(
            listOf(
                target,
                player(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                player(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                player(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
            )
        )

        assertEquals(emptySet(), battle.resolved)
    }

    private fun setupBattle(target: Player): Battle {
        return Battle(playerGridOrder = PlayerGridOrder(SequentialRandomizer())).apply {
            setup(
                listOf(
                    target,
                    player(2, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                    player(3, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1)),
                    player(4, FixedDie(4, 1), FixedDie(6, 1), FixedDie(8, 1))
                )
            )
        }
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
