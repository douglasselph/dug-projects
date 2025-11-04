package dugsolutions.leaf.game.battle.domain

import dugsolutions.leaf.player.Player

data class PlayerValues(
    val player: Player,
    val values: List<DieInsects> = emptyList()
)
