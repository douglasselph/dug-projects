package dugsolutions.leaf.v35.player.decision.learned.cultivation.support

class CultivationSupportFeatureVector private constructor(
    private val values: DoubleArray,
    private val named: Map<String, Double>
) {
    operator fun get(feature: CultivationSupportFeature): Double = values[feature.ordinal]
    fun namedValues(): Map<String, Double> = named
    fun toDoubleArray(): DoubleArray = values.copyOf()

    companion object {
        fun build(block: Builder.() -> Unit): CultivationSupportFeatureVector = Builder().apply(block).build()
    }

    class Builder {
        private val values = DoubleArray(CultivationSupportFeature.entries.size)
        private val named = linkedMapOf<String, Double>()
        operator fun set(feature: CultivationSupportFeature, value: Number) { values[feature.ordinal] = value.toDouble() }
        fun putNamed(key: String, value: Double) {
            require(LearnedCultivationSupportWeights.isNamedFeatureKey(key)) { "Unknown Cultivation Support named feature: $key" }
            named[key] = value
        }
        fun build() = CultivationSupportFeatureVector(values.copyOf(), named.toMap())
    }
}
