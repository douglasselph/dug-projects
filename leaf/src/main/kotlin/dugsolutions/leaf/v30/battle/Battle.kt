package dugsolutions.leaf.v30.battle

import dugsolutions.leaf.v30.battle.domain.BattleGrid
import dugsolutions.leaf.v30.battle.domain.BattleGridSnapshot
import dugsolutions.leaf.v30.battle.domain.BattleItem
import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.battle.domain.Result
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Die

class Battle(
    private val chronicle: Chronicle = GameChronicle(),
    private val playerGridOrder: PlayerGridOrder = PlayerGridOrder(),
    private val battleEvaluator: BattleEvaluator = BattleEvaluator()
) {
    private var _grid: BattleGrid? = null

    val grid: BattleGrid
        get() = _grid ?: throw IllegalStateException("Battle grid has not been setup")

    val isSetup: Boolean
        get() = _grid != null

    fun setup(players: List<Player>): Battle {
        val orderedPlayers = playerGridOrder(players)
        _grid = BattleGrid(orderedPlayers.map { it.id })
        orderedPlayers.forEach { player ->
            setupPlayerColumn(player)
        }
        return this
    }

    fun snapshot(): BattleGridSnapshot {
        return grid.snapshot()
    }

    fun computeWinners(): Result {
        return battleEvaluator(snapshot())
    }

    fun add(
        player: Player,
        row: BattleStrikeRow,
        die: Die
    ): Boolean {
        return add(player, row, BattleItem.DieItem(die))
    }

    fun add(
        player: Player,
        row: BattleStrikeRow,
        critter: Critter
    ): Boolean {
        return add(player, row, BattleItem.CritterItem(critter))
    }

    fun addBulwarkToken(
        player: Player,
        row: BattleStrikeRow
    ): Boolean {
        return add(player, row, BattleItem.BulwarkToken)
    }

    fun setDieValue(
        player: Player,
        row: BattleStrikeRow,
        die: Die,
        value: Int
    ): Boolean {
        val dieItem = grid.getSquare(player.id, row).all
            .filterIsInstance<BattleItem.DieItem>()
            .firstOrNull { it.die == die }
            ?: return false
        dieItem.die.adjustTo(value)
        return true
    }

    fun replaceCritter(
        player: Player,
        from: Critter,
        to: Critter
    ): Int {
        return _grid?.replaceCritter(player.id, from, to) ?: 0
    }

    fun drainCritters(): List<Critter> {
        return _grid?.drainCritters().orEmpty()
    }

    private fun setupPlayerColumn(player: Player) {
        val dice = sortDiceForStrikeRows(player.diceHand.dice)
        if (dice.size != BattleGrid.NUM_STRIKE_ROWS) {
            chronicle(
                Moment.Warning(
                    player = player,
                    type = WarningType.BATTLE_HAND_DICE_COUNT_NOT_THREE,
                    actualCount = dice.size
                )
            )
        }
        dice.take(BattleGrid.NUM_STRIKE_ROWS).forEachIndexed { index, die ->
            grid.add(
                playerId = player.id,
                row = BattleStrikeRow.entries[index],
                item = BattleItem.DieItem(die)
            )
        }
    }

    private fun sortDiceForStrikeRows(dice: List<Die>): List<Die> {
        return dice.sortedWith(
            compareByDescending<Die> { it.value }
                .thenBy { it.sides }
        )
    }

    private fun add(
        player: Player,
        row: BattleStrikeRow,
        item: BattleItem
    ): Boolean {
        val square = grid.getSquare(player.id, row)
        if (!square.canAdd(item)) return false
        square.add(item)
        return true
    }
}
