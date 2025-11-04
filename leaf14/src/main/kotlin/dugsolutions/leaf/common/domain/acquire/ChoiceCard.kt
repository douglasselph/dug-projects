package dugsolutions.leaf.common.domain.acquire

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.DieValues

data class ChoiceCard(
    val card: GameCard,
    val usingDice: DieValues
) {

    override fun toString(): String {
        return "Card(${card.name} for $usingDice)"
    }
}
