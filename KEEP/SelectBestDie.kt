package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.random.die.DieCost
import dugsolutions.leaf.game.acquire.domain.Credits
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.grove.Grove

class SelectBestDie(
    private val grove: Grove,
    private val dieFactory: DieFactory,
    private val dieCost: DieCost
) {

    operator fun invoke(credits: Credits): Die? {
        val pips = credits.pipTotal
        return grove.getAvailableDiceSides()
            .filter { dieCost(it) <= pips }
            .maxByOrNull { it }
            ?.let { dieFactory(it) }
    }
}
