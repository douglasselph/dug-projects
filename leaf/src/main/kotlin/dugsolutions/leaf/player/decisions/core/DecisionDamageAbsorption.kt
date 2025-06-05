package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die

interface DecisionDamageAbsorption {

    data class Result(
        val cards: List<GameCard> = emptyList(),
        val floralCards: List<GameCard> = emptyList(),
        val dice: List<Die> = emptyList()
    ) {
        val allEmpty: Boolean
            get() {
                return cards.isEmpty() && floralCards.isEmpty() && dice.isEmpty()
            }
    }

    suspend operator fun invoke(): Result

}
