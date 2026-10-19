package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.common.domain.acquire.ChoiceCard
import dugsolutions.leaf.player.Player

class EvaluateAcquireCard(
    private val bestCardEvaluator: BestCardEvaluator
) {

    operator fun invoke(player: Player, possibleChoices: List<ChoiceCard>): ChoiceCard? {
        val allCardsOwned = player.allCardsInDeck()
        val possibleCards = possibleChoices.map { it.card }
        if (possibleCards.isEmpty()) {
            return null
        }
        val bestCardOf = bestCardEvaluator(player, possibleCards)
        val winningChoice = possibleChoices.firstOrNull { it.card == bestCardOf } ?: return null
        return ChoiceCard(
            bestCardOf,
            winningChoice.usingDice
        )
    }

}
