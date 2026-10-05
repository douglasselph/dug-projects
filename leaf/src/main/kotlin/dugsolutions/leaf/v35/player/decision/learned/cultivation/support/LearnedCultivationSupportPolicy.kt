package dugsolutions.leaf.v35.player.decision.learned.cultivation.support

import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationSupportActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationSupportDecision
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationSupportPolicy
import dugsolutions.leaf.v35.player.decision.cultivation.stableCultivationSupportId

class LearnedCultivationSupportPolicy(weights: LearnedCultivationSupportWeights) : CultivationSupportPolicy {
    private val scorer = LinearCultivationSupportScorer(weights)

    override fun chooseSupport(request: ChooseCultivationSupportActionRequest): CultivationSupportDecision {
        val candidates = buildList {
            add(CultivationSupportDecision.Pass)
            request.legalActions.forEach { add(CultivationSupportDecision.Use(it)) }
        }
        // Explicit stable identity tie break keeps deterministic results independent of collection implementation.
        return candidates.maxWithOrNull(
            compareBy<CultivationSupportDecision> { scorer.score(CultivationSupportFeatureExtractor.extract(request, it)) }
                .thenByDescending {
                    when (it) {
                        CultivationSupportDecision.Pass -> "CULT_SUPPORT:DONE"
                        is CultivationSupportDecision.Use -> it.action.stableCultivationSupportId()
                    }
                }
        ) ?: CultivationSupportDecision.Pass
    }
}

class LinearCultivationSupportScorer(private val weights: LearnedCultivationSupportWeights) {
    fun score(v: CultivationSupportFeatureVector): Double =
        CultivationSupportFeature.entries.sumOf { v[it] * weights[it] } +
            v.namedValues().entries.sumOf { (k,value) -> value * weights.named(k) }
}
