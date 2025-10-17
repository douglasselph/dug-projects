package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die

interface DecisionDamageAbsorption {

    data class Result(
        val cards: List<GameCard> = emptyList(),
        val floralCards: List<GameCard> = emptyList(),
        val dice: List<Die> = emptyList(),
        val damageToAbsorb: Int = 0
    ) {
        val allEmpty: Boolean
            get() {
                return cards.isEmpty() && floralCards.isEmpty() && dice.isEmpty()
            }

        override fun toString(): String {
            val cardLine = (cards + floralCards).joinToString(",") { it.name }
            val dieLine = dice.joinToString(",") { "D" + it.sides.toString() }

            return "Result(${
                listOfNotNull(
                    cardLine.takeIf { it.isNotEmpty() },
                    dieLine.takeIf { it.isNotEmpty() }
                ).joinToString(" + ")
            }, damage=$damageToAbsorb)"
        }

    }

    suspend operator fun invoke(): Result

}
