package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.grove.Grove

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
