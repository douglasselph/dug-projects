package dugsolutions.leaf.v35.player.decision.learned.battle.main

class BattleMainFeatureVector private constructor(
    private val values: DoubleArray,
    private val named: Map<String, Double>
) {
    operator fun get(feature: BattleMainFeature): Double = values[feature.ordinal]
    fun namedValues(): Map<String, Double> = named
    fun toDoubleArray(): DoubleArray = values.copyOf()

    companion object {
        fun build(block: Builder.() -> Unit): BattleMainFeatureVector = Builder().apply(block).build()
    }

    class Builder {
        private val values = DoubleArray(BattleMainFeature.entries.size)
        private val named = linkedMapOf<String, Double>()
        operator fun set(feature: BattleMainFeature, value: Number) { values[feature.ordinal] = value.toDouble() }
        fun putNamed(key: String, value: Double) {
            require(LearnedBattleMainWeights.isNamedFeatureKey(key)) { "Unknown Battle Main named feature: $key" }
            named[key] = value
        }
        fun build() = BattleMainFeatureVector(values.copyOf(), named.toMap())
    }
}
