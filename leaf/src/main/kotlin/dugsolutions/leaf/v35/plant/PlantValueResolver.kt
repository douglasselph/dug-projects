package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule

/**
 * Explicit per-game seam for effective Plant experiment values.
 *
 * Canonical game construction uses [CANONICAL]. Research configurations may
 * supply another immutable resolver through GameConfig. PlantCard itself stays
 * canonical and never consults process-global state.
 *
 * Cost, availability, structured end-game scoring, and executable effects are resolved here so
 * PlantCard itself remains canonical and never consults experiment state.
 */
interface PlantValueResolver {
    fun costFor(card: PlantCard): Int
    fun isAvailable(card: PlantCard): Boolean
    fun scoringRuleFor(card: PlantCard): PlantScoringRule = card.scoringRule
    fun effectFor(card: PlantCard): GameEffect = card.effect

    companion object {
        val CANONICAL: PlantValueResolver = object : PlantValueResolver {
            override fun costFor(card: PlantCard): Int = card.cost
            override fun isAvailable(card: PlantCard): Boolean = true
            override fun scoringRuleFor(card: PlantCard): PlantScoringRule = card.scoringRule
            override fun effectFor(card: PlantCard): GameEffect = card.effect
            override fun toString(): String = "PlantValueResolver.CANONICAL"
        }
    }
}
