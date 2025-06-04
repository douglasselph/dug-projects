package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.getFlourishTypes
import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.local.EvaluateCardPurchases

class PossibleCards(
    private val evaluateCardPurchases: EvaluateCardPurchases
) {

    operator fun invoke(
        player: Player,
        combinations: Combinations,
        marketCards: List<GameCard>
    ): List<ChoiceCard> {
        val flourishTypesHeld = player.cardsInHand.getFlourishTypes()
        val choices = mutableListOf<ChoiceCard>()

        for (combination in combinations) {
            val possibleCards = evaluateCardPurchases(marketCards, flourishTypesHeld, combination, player.effectsList)
            for (card in possibleCards) {
                choices.add(ChoiceCard(card, combination))
            }
        }
        if (choices.isEmpty()) {
            return emptyList()
        }
        simplify(choices)

        return choices
    }

    private fun simplify(choices: MutableList<ChoiceCard>) {
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
