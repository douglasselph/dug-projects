package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.market.Market

class SelectPossibleDice(
    private val market: Market,
    private val dieFactory: DieFactory
)  {

    operator fun invoke(): List<Die> {
        val sides = market.getAvailableDiceSides()
        val result = mutableListOf<Die>()
        for (side in sides) {
            result.add(dieFactory(side))
        }
        return result
    }
}
