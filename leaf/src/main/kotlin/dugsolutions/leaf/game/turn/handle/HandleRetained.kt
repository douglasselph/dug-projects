package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.HandItem

/**
 * This must be called after drawHand()
 */
class HandleRetained {

    operator fun invoke(player: Player) {
        player.addCardsToHand(player.retained.filterIsInstance<HandItem.aCard>().map { it.card.id })
        player.addDiceToHand(player.retained.filterIsInstance<HandItem.aDie>().map { it.die })
        player.retained.clear()
    }

}
