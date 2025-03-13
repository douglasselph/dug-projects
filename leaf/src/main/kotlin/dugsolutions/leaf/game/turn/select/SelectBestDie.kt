package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.game.purchase.domain.Credits
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.market.Market

class SelectBestDie(
    private val market: Market,
    private val dieFactory: DieFactory,
    private val dieCost: DieCost
) {

    operator fun invoke(credits: Credits): Die? {
        val pips = credits.pipTotal
        return market.getAvailableDiceSides()
            .filter { dieCost(it) <= pips }
            .maxByOrNull { it }
            ?.let { dieFactory(it) }
    }
}
