package dugsolutions.leaf.grove

import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Die

class SelectPossibleDice(
    private val grove: Grove,
    private val dieFactory: DieFactory
) {

    operator fun invoke(): List<Die> {
        val sides = grove.getAvailableDiceSides()
        val result = mutableListOf<Die>()
        for (side in sides) {
            result.add(dieFactory(side))
        }
        return result
    }
}
