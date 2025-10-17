package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.grove.Grove

class SelectAllDice(
    private val grove: Grove,
    private val dieFactory: DieFactory
) {

    operator fun invoke(): Dice {
        val sides = grove.getAvailableDiceSides()
        val dice = Dice()
        for (side in sides) {
            val count = grove.getDiceQuantity(side)
            repeat(count) { dice.add(dieFactory(side)) }
        }
        return dice
    }
}
