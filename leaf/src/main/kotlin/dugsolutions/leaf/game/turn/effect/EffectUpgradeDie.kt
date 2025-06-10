package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.handle.HandleDieUpgrade
import dugsolutions.leaf.game.turn.handle.HandleLimitedDieUpgrade
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.DieSides

class EffectUpgradeDie(
    private val handleLimitedDieUpgrade: HandleLimitedDieUpgrade,
    private val handleDieUpgrade: HandleDieUpgrade,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, only: List<DieSides> = emptyList(), discardAfterUse: Boolean = false) {
        val die = if (only.isNotEmpty()) {
            handleLimitedDieUpgrade(player, only, discardAfterUse)
        } else {
            handleDieUpgrade(player, discardAfterUse)
        }
        die?.let {
            chronicle(Moment.UPGRADE_DIE(player, die))
        }
    }

}
