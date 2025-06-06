package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.player.Player

class AcquireCardEvaluator(
    private val bestCardEvaluator: BestCardEvaluator
) {

    operator fun invoke(player: Player, possibleChoices: List<ChoiceCard>): ChoiceCard? {
        val possibleCards = possibleChoices.map { it.card }
        val bestCardOf = bestCardEvaluator(player, possibleCards)
        val winningChoice = possibleChoices.firstOrNull { it.card == bestCardOf } ?: return null
        return ChoiceCard(
            bestCardOf,
            winningChoice.combination
        )
    }

}
