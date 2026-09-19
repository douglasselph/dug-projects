package dugsolutions.leaf.v14.game.turn.handle

import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.domain.HandItem

/**
 * This must be called after drawHand()
 */
class HandleRetained {

    operator fun invoke(player: Player): Int {
        val count = player.retained.size
        player.addCardsToHand(player.retained.filterIsInstance<HandItem.aCard>().map { it.card.id })
        player.addDiceToHand(player.retained.filterIsInstance<HandItem.aDie>().map { it.die })
        player.retained.clear()
        return count
    }

}
