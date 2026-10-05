package dugsolutions.leaf.v35.player.decision.learned.plant

class PlantEffectFeatureVector(
    private val values: DoubleArray,
    private val named: Map<String, Double>
) {
    init { require(values.size == PlantEffectFeature.entries.size) }
    operator fun get(feature: PlantEffectFeature): Double = values[feature.ordinal]
    fun namedValues(): Map<String, Double> = named
}
