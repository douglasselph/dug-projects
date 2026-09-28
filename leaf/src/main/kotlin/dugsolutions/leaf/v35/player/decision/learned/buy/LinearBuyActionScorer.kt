package dugsolutions.leaf.v35.player.decision.learned.buy

data class BuyScoreContribution(val feature: String, val value: Double, val weight: Double) { val contribution: Double get()=value*weight }
data class BuyActionScore(val total: Double, val contributions: List<BuyScoreContribution>)

class LinearBuyActionScorer(private val weights: LearnedBuyWeights) {
    fun score(features: BuyFeatureVector): BuyActionScore {
        val standard=BuyFeature.entries.map { BuyScoreContribution(it.name,features[it],weights[it]) }
        val named=features.namedValues().map { (key,value)->BuyScoreContribution(key,value,weights.named(key)) }
        val parts=standard+named
        return BuyActionScore(parts.sumOf{it.contribution},parts.filter{it.value!=0.0&&it.weight!=0.0})
    }
}
