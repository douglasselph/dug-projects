package dugsolutions.leaf.v35.player.decision.learned.buy

class BuyFeatureVector private constructor(private val values: DoubleArray) {
    init { require(values.size == BuyFeature.entries.size) }
    operator fun get(feature: BuyFeature): Double = values[feature.ordinal]
    fun asMap(): Map<BuyFeature, Double> = BuyFeature.entries.associateWith { get(it) }

    companion object {
        fun build(block: MutableMap<BuyFeature, Double>.() -> Unit): BuyFeatureVector {
            val map = mutableMapOf<BuyFeature, Double>().apply(block)
            return BuyFeatureVector(DoubleArray(BuyFeature.entries.size) { map[BuyFeature.entries[it]] ?: 0.0 })
        }
    }
}
