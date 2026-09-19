package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.player.Player

class CheckRefresh {

    operator fun invoke(player: Player): Boolean {
        val creatureCards = player.creatureCards
        if (creatureCards.isEmpty()) return false
        if (creatureCards.any { it.isFaceUp }) return false

        player.flipAllCreatureCardsFaceUp()
        return true
    }
}
