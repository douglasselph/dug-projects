package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.player.Player

interface DecisionDamageAbsorption {

    data class Result(
        val cardIds: List<CardID>,
        val dice: List<Die>
    )

    operator fun invoke(): Result?

}
