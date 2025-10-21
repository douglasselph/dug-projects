package dugsolutions.leaf.game.acquire.cost

import dugsolutions.leaf.common.domain.acquire.UsingDice
import dugsolutions.leaf.random.die.MissingDieException
import dugsolutions.leaf.player.Player

open class ApplyCost{

    open operator fun invoke(
        player: Player,
        usingDice: UsingDice,
        acquireItem: (player: Player) -> Unit = {}
    ) {
        payFor(player, usingDice)
        acquireItem(player)
    }

    private fun payFor(player: Player, usingDice: UsingDice) {
        for (die in usingDice.values) {
            if (!player.discard(die)) {
                throw MissingDieException("Could not discard the die $die")
            }
        }
    }
}
