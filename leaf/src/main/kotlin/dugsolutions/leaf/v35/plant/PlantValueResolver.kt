package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType

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
    fun typeFor(card: PlantCard): PlantType = card.type
    fun costFor(card: PlantCard): Int
    fun isAvailable(card: PlantCard): Boolean
    fun scoringRuleFor(card: PlantCard): PlantScoringRule = card.scoringRule
    fun effectFor(card: PlantCard): GameEffect = card.effect

    /** Materialized effective card for research setup and downstream type-sensitive rules. */
    fun effectiveCardFor(card: PlantCard): PlantCard {
        val type = typeFor(card)
        val cost = costFor(card)
        val scoringRule = scoringRuleFor(card)
        val effect = effectFor(card)
        return if (type == card.type && cost == card.cost && scoringRule == card.scoringRule && effect == card.effect) {
            card
        } else {
            card.copy(type = type, cost = cost, scoringRule = scoringRule, effect = effect)
        }
    }

    companion object {
        val CANONICAL: PlantValueResolver = object : PlantValueResolver {
            override fun typeFor(card: PlantCard): PlantType = card.type
            override fun costFor(card: PlantCard): Int = card.cost
            override fun isAvailable(card: PlantCard): Boolean = true
            override fun scoringRuleFor(card: PlantCard): PlantScoringRule = card.scoringRule
            override fun effectFor(card: PlantCard): GameEffect = card.effect
            override fun toString(): String = "PlantValueResolver.CANONICAL"
        }
    }
}
