package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.main.domain.DiceInfo

class GatherDiceInfo {

    operator fun invoke(incoming: Dice, values: Boolean): DiceInfo {

        return if (values) {
            // Dice "D4=1" style
            DiceInfo(
                incoming.dice.map { "D${it.sides}=${it.value}" }
            )
        } else {
            // Dice as "4D4 3D6" style
            DiceInfo(incoming.dice
                .groupBy { it.sides }
                .map { (sides, dice) -> "${dice.size}D$sides" }
            )
        }
    }

}
