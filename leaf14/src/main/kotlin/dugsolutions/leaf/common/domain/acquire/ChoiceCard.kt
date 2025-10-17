package dugsolutions.leaf.common.domain.acquire

import dugsolutions.leaf.cards.domain.GameCard

data class ChoiceCard(
    val card: GameCard,
    val usingDice: UsingDice
) {

    override fun toString(): String {
        return "Card(${card.name} for $usingDice)"
    }
}
