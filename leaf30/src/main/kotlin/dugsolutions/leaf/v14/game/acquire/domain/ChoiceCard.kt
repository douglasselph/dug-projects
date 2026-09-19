package dugsolutions.leaf.v14.game.acquire.domain

import dugsolutions.leaf.v14.cards.domain.GameCard

data class ChoiceCard(
    val card: GameCard,
    val combination: Combination
) {

    override fun toString(): String {
        return "Card(${card.name} for $combination)"
    }
}
