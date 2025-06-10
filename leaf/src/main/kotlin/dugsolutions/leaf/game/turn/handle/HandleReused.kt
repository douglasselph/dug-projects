package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.HandItem

class HandleReused {

    operator fun invoke(player: Player) {
        player.reused.forEach { item ->
            when(item) {
                is HandItem.aCard -> {
                    player.addCardToHand(item.card.id)
                    player.removeCardFromCompost(item.card.id)
                }
                is HandItem.aDie -> {
                    player.addDieToHand(item.die)
                    player.removeDieFromCompost(item.die)
                }
            }
        }
        player.reused.clear()
    }
}
