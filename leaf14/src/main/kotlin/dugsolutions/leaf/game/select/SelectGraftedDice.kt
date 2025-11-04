package dugsolutions.leaf.game.select

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.Die

class SelectGraftedDice {

    suspend operator fun invoke(player: Player): List<Die> {
        return emptyList()
    }

}
