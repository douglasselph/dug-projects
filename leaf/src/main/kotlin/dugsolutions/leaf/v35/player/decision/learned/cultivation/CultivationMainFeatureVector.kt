package dugsolutions.leaf.v35.player.decision.learned.cultivation

/** Immutable dense standard features plus sparse stable named features. */
class CultivationMainFeatureVector private constructor(
    private val standardValues: DoubleArray,
    private val namedValues: Map<String, Double>
) {
    init {
        require(standardValues.size == CultivationMainFeature.entries.size)
    }

    operator fun get(feature: CultivationMainFeature): Double = standardValues[feature.ordinal]
    fun namedValue(key: String): Double = namedValues[key] ?: 0.0
    fun namedValues(): Map<String, Double> = namedValues.toMap()
    fun asMap(): Map<CultivationMainFeature, Double> =
        CultivationMainFeature.entries.associateWith { get(it) }

    companion object {
        fun build(block: Builder.() -> Unit): CultivationMainFeatureVector {
            val builder = Builder().apply(block)
            return CultivationMainFeatureVector(
                standardValues = DoubleArray(CultivationMainFeature.entries.size) { index ->
                    builder.standard[CultivationMainFeature.entries[index]] ?: 0.0
                },
                namedValues = builder.named.toMap()
            )
        }
    }

    class Builder internal constructor() {
        internal val standard = linkedMapOf<CultivationMainFeature, Double>()
        internal val named = linkedMapOf<String, Double>()

        operator fun set(feature: CultivationMainFeature, value: Double) {
            require(value.isFinite()) { "Cultivation Main feature $feature must be finite: $value" }
            standard[feature] = value
        }

        operator fun set(feature: CultivationMainFeature, value: Number) {
            set(feature, value.toDouble())
        }

        fun putNamed(key: String, value: Double) {
            require(LearnedCultivationMainWeights.isNamedFeatureKey(key)) {
                "Invalid Cultivation Main named feature: $key"
            }
            require(value.isFinite()) { "Cultivation Main named feature $key must be finite: $value" }
            named[key] = value
        }
    }
}
