package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule

/**
 * Research-only overrides for one canonical Plant definition.
 *
 * Null dimensions mean "leave the canonical card definition unchanged". This
 * model deliberately keeps cost, Grove availability, and structured end-game
 * scoring independent so experiments can change one dimension at a time.
 */
data class PlantExperimentOverride(
    val cost: Int? = null,
    val available: Boolean? = null,
    val scoringRule: PlantScoringRule? = null
) {
    init {
        require(cost == null || cost >= 0) {
            "Experimental Plant cost must be non-negative: $cost"
        }
    }
}

/**
 * Immutable research configuration keyed by the Plant CSV `name` field, which
 * is the stable Plant card ID used by [dugsolutions.leaf.v35.plant.PlantCardRegistry].
 *
 * This object contains no global state and does not mutate [PlantCard]. With no
 * matching override, all resolver helpers return canonical behavior.
 */
class PlantExperimentConfig private constructor(
    overrides: Map<String, PlantExperimentOverride>
) {
    private val overridesByPlantId: Map<String, PlantExperimentOverride> =
        overrides.entries.associate { (plantId, override) ->
            plantId.normalizedPlantId() to override
        }

    val isEmpty: Boolean
        get() = overridesByPlantId.isEmpty()

    /** Returns only an explicitly configured intervention, or null. */
    fun overrideFor(plantId: String): PlantExperimentOverride? =
        overridesByPlantId[plantId.normalizedPlantId()]

    /** Convenience lookup using the canonical card's stable ID. */
    fun overrideFor(card: PlantCard): PlantExperimentOverride? =
        overrideFor(card.name)

    /** Experimental cost when supplied; otherwise the canonical CSV cost. */
    fun costFor(card: PlantCard): Int =
        overrideFor(card)?.cost ?: card.cost

    /** Experimental Grove availability when supplied; otherwise canonical availability. */
    fun isAvailable(card: PlantCard): Boolean =
        overrideFor(card)?.available ?: true

    /** Experimental typed scoring rule when supplied; otherwise the canonical rule. */
    fun scoringRuleFor(card: PlantCard): PlantScoringRule =
        overrideFor(card)?.scoringRule ?: card.scoringRule

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is PlantExperimentConfig && overridesByPlantId == other.overridesByPlantId)

    override fun hashCode(): Int = overridesByPlantId.hashCode()

    override fun toString(): String =
        "PlantExperimentConfig(overridesByPlantId=$overridesByPlantId)"

    companion object {
        /** Canonical game behavior: no Plant experiment interventions. */
        val EMPTY: PlantExperimentConfig = PlantExperimentConfig(emptyMap())

        fun of(overrides: Map<String, PlantExperimentOverride>): PlantExperimentConfig {
            if (overrides.isEmpty()) return EMPTY

            val normalized = linkedMapOf<String, PlantExperimentOverride>()
            overrides.forEach { (plantId, override) ->
                val key = plantId.normalizedPlantId()
                require(key !in normalized) {
                    "Duplicate Plant experiment override ID after normalization: '$plantId'"
                }
                normalized[key] = override
            }
            return PlantExperimentConfig(normalized.toMap())
        }

        fun of(vararg overrides: Pair<String, PlantExperimentOverride>): PlantExperimentConfig =
            of(overrides.toMap())
    }
}

private fun String.normalizedPlantId(): String {
    val normalized = trim().lowercase()
    require(normalized.isNotEmpty()) { "Plant experiment override ID cannot be blank" }
    return normalized
}
