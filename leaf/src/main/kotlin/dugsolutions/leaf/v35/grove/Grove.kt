package dugsolutions.leaf.v35.grove

import dugsolutions.leaf.v35.grove.plant.PlantMarket
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.tokens.Butterflies
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Critters
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.tokens.Tokens
import dugsolutions.leaf.v35.wisp.WispDeck

/**
 * Shared mutable component state for one game.
 *
 * Grove owns its child inventories but deliberately does not duplicate their
 * APIs with forwarding methods. Gameplay coordinators operate through:
 *
 * - plantMarket
 * - graftBed
 * - critters
 * - tokens
 * - butterflies
 * - wispDeck
 *
 * Grove does not own Players, Decisions, Chronicle, Battle state, Round state,
 * or actual rollable Die instances.
 */
class Grove(
    selectedPlantCards: List<PlantCard>,
    val wispDeck: WispDeck
) {

    companion object {
        const val CRITTERS_PER_TYPE = 9
        const val TOKENS_PER_TYPE = 9
    }

    val plantMarket =
        PlantMarket(selectedPlantCards)

    val graftBed =
        GraftBed()

    val critters =
        Critters()

    val tokens =
        Tokens()

    val butterflies =
        Butterflies()

    init {
        reset()
    }

    /**
     * Restores this Grove to its complete initial state while preserving the
     * originally selected Plant definitions.
     */
    fun reset() {
        plantMarket.reset()
        graftBed.reset()

        critters.clear()
        critters.set(
            Critter.BEE,
            CRITTERS_PER_TYPE
        )
        critters.set(
            Critter.WORM,
            CRITTERS_PER_TYPE
        )

        tokens.reset(
            waterCount = TOKENS_PER_TYPE,
            mulchTokens = List(TOKENS_PER_TYPE) {
                Token.MULCH()
            }
        )

        butterflies.clear()
        Butterfly.entries.forEach {
            butterflies.add(it)
        }

        wispDeck.reset()
    }
}
