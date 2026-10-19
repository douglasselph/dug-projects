package dugsolutions.leaf.v14.game.turn.select

import dugsolutions.leaf.v14.random.die.Dice
import dugsolutions.leaf.v14.random.di.DieFactory
import dugsolutions.leaf.v14.grove.Grove

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
