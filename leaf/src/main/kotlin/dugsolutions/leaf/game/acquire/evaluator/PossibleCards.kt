package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.getFlourishTypes
import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.local.CanPurchaseCards

class PossibleCards(
    private val canPurchaseCards: CanPurchaseCards
) {

    operator fun invoke(
        player: Player,
        combinations: Combinations,
        marketCards: List<GameCard>
    ): List<ChoiceCard> {
        val flourishTypesHeld = player.cardsInHand.getFlourishTypes()
        val choices = mutableListOf<ChoiceCard>()

        for (combination in combinations) {
            val possibleCards = canPurchaseCards(marketCards, flourishTypesHeld, combination, player.delayedEffectList)
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
