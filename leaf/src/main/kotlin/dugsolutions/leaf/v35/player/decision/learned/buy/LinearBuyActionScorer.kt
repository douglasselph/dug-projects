package dugsolutions.leaf.v35.player.decision.learned.buy

data class BuyScoreContribution(val feature: BuyFeature, val value: Double, val weight: Double) {
    val contribution: Double get() = value * weight
}
data class BuyActionScore(val total: Double, val contributions: List<BuyScoreContribution>)

class LinearBuyActionScorer(private val weights: LearnedBuyWeights) {
    fun score(features: BuyFeatureVector): BuyActionScore {
        val parts = BuyFeature.entries.map { BuyScoreContribution(it, features[it], weights[it]) }
        return BuyActionScore(parts.sumOf { it.contribution }, parts.filter { it.value != 0.0 && it.weight != 0.0 })
    }
}
