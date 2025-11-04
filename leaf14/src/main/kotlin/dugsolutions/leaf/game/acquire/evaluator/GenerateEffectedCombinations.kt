package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.player.Player

class GenerateEffectedCombinations(
    private val possibleCardsToExecute: PossibleCardsToExecute
) {

    // Extend the incoming dice combinations by all the possible ways we can apply the card effects to the dice
    operator fun invoke(player: Player, combinations: Combinations): Combinations {
        val cardsToExecute = possibleCardsToExecute(player)
        return combinations
    }

}
