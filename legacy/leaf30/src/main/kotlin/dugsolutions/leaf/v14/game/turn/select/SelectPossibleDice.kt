package dugsolutions.leaf.v14.game.turn.select

import dugsolutions.leaf.v14.random.die.Die
import dugsolutions.leaf.v14.random.di.DieFactory
import dugsolutions.leaf.v14.grove.Grove

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
