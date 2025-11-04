package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.game.acquire.domain.CardCombination
import dugsolutions.leaf.player.Player

class PossibleCardsToExecute {

    // generate all the possible card groups that can be executed on the player's plant creature
    operator fun invoke(player: Player): List<CardCombination> {
        return emptyList()
    }

}
