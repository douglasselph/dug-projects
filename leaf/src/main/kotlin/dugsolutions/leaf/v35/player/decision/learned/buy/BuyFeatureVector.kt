package dugsolutions.leaf.v35.player.decision.learned.buy

class BuyFeatureVector private constructor(private val values: DoubleArray, private val named: Map<String, Double>) {
    init { require(values.size == BuyFeature.entries.size) }
    operator fun get(feature: BuyFeature): Double = values[feature.ordinal]
    fun namedValue(key: String): Double = named[key] ?: 0.0
    fun namedValues(): Map<String, Double> = named.toMap()
    fun asMap(): Map<BuyFeature, Double> = BuyFeature.entries.associateWith { get(it) }

    companion object {
        fun build(block: Builder.() -> Unit): BuyFeatureVector {
            val b=Builder().apply(block)
            return BuyFeatureVector(DoubleArray(BuyFeature.entries.size){ b.standard[BuyFeature.entries[it]]?:0.0 }, b.named.toMap())
        }
    }
    class Builder internal constructor() {
        internal val standard=mutableMapOf<BuyFeature,Double>(); internal val named=mutableMapOf<String,Double>()
        operator fun set(feature:BuyFeature,value:Double){standard[feature]=value}
        operator fun get(feature:BuyFeature):Double?=standard[feature]
        fun putNamed(key:String,value:Double){ require(LearnedBuyWeights.isNamedFeatureKey(key)); named[key]=value }
    }
}
