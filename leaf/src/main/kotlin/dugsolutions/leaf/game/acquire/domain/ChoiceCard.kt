package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.cards.domain.GameCard

data class ChoiceCard(
    val card: GameCard,
    val combination: Combination
) {

    override fun toString(): String {
        return "Card(${card.name} for $combination)"
    }
}
