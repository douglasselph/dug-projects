package dugsolutions.leaf.common.domain.acquire

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.random.die.DieValues

data class Combination(
    val values: DieValues = DieValues(emptyList()), // List of die-values from hand post-adjusted after used cards
    val using: CardCombination = CardCombination(emptyList(), emptyList()) // what we needed to use in order to generate the die values.
) {
    val totalValue: Int
        get() {
            val diceTotal = values.dice.sumOf { it.value }
            return diceTotal
        }
}

data class CardCombination(
    val usedCards: List<GameCard>, // cards with the effects applied to the dice to adjust them.
    val usedTokens: List<Token>    // the tokens that were used in order to apply the effects from the cards.
)
