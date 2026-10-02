package dugsolutions.leaf.v35.player.decision.learned.cultivation

data class CultivationMainScoreContribution(
    val feature: String,
    val value: Double,
    val weight: Double
) {
    val contribution: Double get() = value * weight
}

data class CultivationMainActionScore(
    val total: Double,
    val contributions: List<CultivationMainScoreContribution>
)

class LinearCultivationMainActionScorer(private val weights: LearnedCultivationMainWeights) {
    fun score(features: CultivationMainFeatureVector): CultivationMainActionScore {
        val standard = CultivationMainFeature.entries.map {
            CultivationMainScoreContribution(it.name, features[it], weights[it])
        }
        val named = features.namedValues().map { (key, value) ->
            CultivationMainScoreContribution(key, value, weights.named(key))
        }
        val parts = standard + named
        return CultivationMainActionScore(
            total = parts.sumOf { it.contribution },
            contributions = parts.filter { it.value != 0.0 && it.weight != 0.0 }
        )
    }
}
