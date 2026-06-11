package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.MainAction
import dugsolutions.leaf.v30.table.Table

abstract class GameCardEffectExecutorBase(
    protected val chronicle: Chronicle = GameChronicle()
) {

    protected open fun gainWormAndBoostWorms(
        table: Table,
        player: Player,
        action: MainAction.ExecuteCard
    ) {
        if (table.grove.has(Critter.WORM)) {
            table.grove.remove(Critter.WORM)
            player.addCritter(Critter.WORM)
        }
        player.replaceCritter(Critter.WORM, Critter.BOOSTED_WORM)
    }
}
