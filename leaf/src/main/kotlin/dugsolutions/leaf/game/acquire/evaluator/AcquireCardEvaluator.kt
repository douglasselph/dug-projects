package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.getFlourishTypes
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.player.Player

class AcquireCardEvaluator(
    private val evaluateCardPurchases: EvaluateCardPurchases
) {

    data class Choice(
        val card: GameCard,
        val combination: Combination
    )

    suspend operator fun invoke(
        player: Player,
        combinations: Combinations,
        marketCards: List<GameCard>
    ): Choice? {
        val flourishTypesHeld = player.cardsInHand.getFlourishTypes()
        val choices = mutableListOf<Choice>()
        
        for (combination in combinations) {
            val possibleCards = evaluateCardPurchases(marketCards, flourishTypesHeld, combination)
            for (card in possibleCards) {
                choices.add(Choice(card, combination))
            }
        }
        if (choices.isEmpty()) {
            return null
        }
        simplify(choices)

        val possibleCards = choices.map { it.card }
        val bestCardOf = player.decisionDirector.bestCardPurchase(possibleCards)
        val winningChoice = choices.firstOrNull { it.card == bestCardOf } ?: return null
        return Choice(
            bestCardOf,
            winningChoice.combination
        )
    }

    private fun simplify(choices: MutableList<Choice>) {
        // Group choices by card
        val choicesByCard = choices.groupBy { it.card }
        
        // For each card, keep only the choice with the lowest simplicity score
        val simplifiedChoices = choicesByCard.map { (_, cardChoices) ->
            cardChoices.minByOrNull { it.combination.simplicityScore }
        }.filterNotNull()
        
        // Clear and update the original list
        choices.clear()
        choices.addAll(simplifiedChoices)
    }

}
