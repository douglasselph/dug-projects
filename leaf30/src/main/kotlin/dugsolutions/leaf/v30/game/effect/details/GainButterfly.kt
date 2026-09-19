package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.common.Butterfly
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.table.Table

class GainButterfly(
    private val table: Table
) {
    operator fun invoke(
        player: Player,
        butterfly: Butterfly
    ): Boolean {
        if (table.grove.has(butterfly)) {
            table.grove.remove(butterfly)
        } else {
            table.players.firstOrNull { it.butterflies.contains(butterfly) }?.remove(butterfly)
        }

        player.remove(butterfly)
        player.add(butterfly)
        return true
    }
}
