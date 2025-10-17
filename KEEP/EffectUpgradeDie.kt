package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.handle.HandleDieUpgrade
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.DieSides

class EffectUpgradeDie(
    private val handleDieUpgrade: HandleDieUpgrade,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, only: List<DieSides> = emptyList(), discardAfterUse: Boolean = false) {
        val die = handleDieUpgrade(player, discardAfterUse, only)

        die?.let {
            chronicle(Moment.UPGRADE_DIE(player, die))
        }
    }

}
