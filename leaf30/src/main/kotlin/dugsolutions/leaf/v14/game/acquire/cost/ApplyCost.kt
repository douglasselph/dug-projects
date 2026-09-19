package dugsolutions.leaf.v14.game.acquire.cost

import dugsolutions.leaf.v14.random.die.MissingDieException
import dugsolutions.leaf.v14.game.acquire.domain.Combination
import dugsolutions.leaf.v14.player.Player

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
