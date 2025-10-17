package dugsolutions.leaf.game.acquire.cost

import dugsolutions.leaf.random.die.MissingDieException
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.Player

open class ApplyCost{

    open operator fun invoke(
        player: Player,
        combination: Combination,
        acquireItem: (player: Player) -> Unit = {}
    ) {
        payFor(player, combination)
        acquireItem(player)
    }

    private fun payFor(player: Player, combination: Combination) {
        for (die in combination.values) {
            if (!player.discard(die)) {
                throw MissingDieException("Could not discard the die $die")
            }
        }
    }
}
