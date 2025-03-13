package dugsolutions.leaf.game.purchase.cost

import dugsolutions.leaf.components.die.MissingDieException
import dugsolutions.leaf.game.purchase.domain.Combination
import dugsolutions.leaf.player.Player

open class ApplyCost(
    private val applyEffects: ApplyEffects
) {

    open operator fun invoke(
        player: Player,
        combination: Combination,
        acquireItem: (player: Player) -> Unit = {}
    ) {
        payFor(player, combination)
        acquireItem(player)
    }

    private fun payFor(player: Player, combination: Combination) {
        applyEffects(player, combination)
        for (die in combination.values) {
            if (!player.discard(die)) {
                throw MissingDieException("Could not discard the die $die")
            }
        }
    }
}
