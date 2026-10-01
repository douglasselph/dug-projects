package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.plant.domain.PlantCard

/**
 * Explicit per-game seam for effective Plant acquisition values.
 *
 * Canonical game construction uses [CANONICAL]. Research configurations may
 * supply another immutable resolver through GameConfig. PlantCard itself stays
 * canonical and never consults process-global state.
 *
 * End-game scoring is deliberately not part of this seam yet.
 */
interface PlantValueResolver {
    fun costFor(card: PlantCard): Int
    fun isAvailable(card: PlantCard): Boolean

    companion object {
        val CANONICAL: PlantValueResolver = object : PlantValueResolver {
            override fun costFor(card: PlantCard): Int = card.cost
            override fun isAvailable(card: PlantCard): Boolean = true
            override fun toString(): String = "PlantValueResolver.CANONICAL"
        }
    }
}
