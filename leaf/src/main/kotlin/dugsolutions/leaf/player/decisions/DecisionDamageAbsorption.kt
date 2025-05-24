package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die

interface DecisionDamageAbsorption {

    data class Result(
        val cards: List<GameCard>,
        val floralCards: List<GameCard>,
        val dice: List<Die>
    )

    operator fun invoke(): Result?

}
