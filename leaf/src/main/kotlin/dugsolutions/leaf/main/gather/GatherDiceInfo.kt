package dugsolutions.leaf.main.gather

import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.main.domain.DiceInfo
import dugsolutions.leaf.main.domain.DieInfo

class GatherDiceInfo {

    operator fun invoke(incoming: Dice, values: Boolean): DiceInfo {

        return if (values) {
            // Dice "D4=1" style
            DiceInfo(
                incoming.dice.mapIndexed { index, die ->
                    DieInfo(
                        index = index,
                        value = "D${die.sides}=${die.value}",
                        backingDie = die
                    )
                }
            )
        } else {
            // Dice as "4D4 3D6" style
            DiceInfo(incoming.dice
                .groupBy { it.sides }
                .map { (sides, dice) -> DieInfo(value = "${dice.size}D$sides") }
            )
        }
    }

}
