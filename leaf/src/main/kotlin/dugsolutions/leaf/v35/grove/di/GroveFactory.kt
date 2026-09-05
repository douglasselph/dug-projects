package dugsolutions.leaf.v35.grove.di

import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import dugsolutions.leaf.v35.wisp.domain.WispCard

/**
 * Creates one independent mutable Grove for one game.
 *
 * WispCardManager is immutable/catalog-like after application data loading and
 * may be shared. Randomizer is supplied per invocation so game construction
 * controls random lifetime/seeding explicitly.
 */
class GroveFactory(
    private val wispCardManager: WispCardManager
) {

    operator fun invoke(
        selectedPlantCards: List<PlantCard>,
        randomizer: Randomizer,
        exactWispCards: List<WispCard>? = null
    ): Grove =
        Grove(
            selectedPlantCards = selectedPlantCards,
            wispDeck = WispDeck(
                wispCardManager = wispCardManager,
                randomizer = randomizer,
                exactResetCards = exactWispCards
            )
        )
}
