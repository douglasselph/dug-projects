package dugsolutions.leaf.v35.player.decision.learned.battle

class BattleSupportFeatureVector private constructor(
    private val values: DoubleArray,
    private val named: Map<String, Double>
) {
    operator fun get(feature: BattleSupportFeature): Double = values[feature.ordinal]
    fun namedValues(): Map<String, Double> = named
    fun toDoubleArray(): DoubleArray = values.copyOf()

    companion object {
        fun build(block: Builder.() -> Unit): BattleSupportFeatureVector = Builder().apply(block).build()
    }

    class Builder {
        private val values = DoubleArray(BattleSupportFeature.entries.size)
        private val named = linkedMapOf<String, Double>()
        operator fun set(feature: BattleSupportFeature, value: Number) { values[feature.ordinal] = value.toDouble() }
        fun putNamed(key: String, value: Double) {
            require(LearnedBattleSupportWeights.isNamedFeatureKey(key)) { "Unknown Battle Support named feature: $key" }
            named[key] = value
        }
        fun build() = BattleSupportFeatureVector(values.copyOf(), named.toMap())
    }
}
