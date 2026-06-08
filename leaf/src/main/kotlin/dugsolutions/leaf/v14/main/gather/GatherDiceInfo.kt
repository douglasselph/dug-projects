package dugsolutions.leaf.v14.main.gather

import dugsolutions.leaf.v14.random.die.Dice
import dugsolutions.leaf.v14.main.domain.DiceInfo
import dugsolutions.leaf.v14.main.domain.DieInfo

class GatherDiceInfo {

    operator fun invoke(incoming: Dice, values: Boolean): DiceInfo {

        return if (values) {
            // Dice "D4=1" style
            DiceInfo(
                incoming.dice.mapIndexed { index, die ->
                    DieInfo(
                        index = index,
                        value = die.toValue(),
                        backingDie = die
                    )
                }
            )
        } else {
            // Dice as "4D4 3D6" style
            DiceInfo(
                incoming.dice
                .groupBy { it.sides }
                .map { (sides, dice) -> DieInfo(value = "${dice.size}D$sides") }
            )
        }
    }

}
