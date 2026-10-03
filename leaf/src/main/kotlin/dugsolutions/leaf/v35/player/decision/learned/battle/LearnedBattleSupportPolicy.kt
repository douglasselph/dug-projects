package dugsolutions.leaf.v35.player.decision.learned.battle

import dugsolutions.leaf.v35.player.decision.battle.BattleSupportPolicy
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleSupportActionRequest
import dugsolutions.leaf.v35.player.decision.battle.stableBattleSupportCandidateId

class LearnedBattleSupportPolicy(weights: LearnedBattleSupportWeights) : BattleSupportPolicy {
    private val scorer = LinearBattleSupportScorer(weights)
    override fun chooseSupport(request: ChooseBattleSupportActionRequest): BattleTurnAction =
        request.legalActions.maxWithOrNull(
            compareBy<BattleTurnAction> { scorer.score(BattleSupportFeatureExtractor.extract(request, it)) }
                .thenByDescending { it.stableBattleSupportCandidateId().stableId }
        ) ?: error("Battle Support learner received no legal action")
}

class LinearBattleSupportScorer(private val weights: LearnedBattleSupportWeights) {
    fun score(v: BattleSupportFeatureVector): Double =
        BattleSupportFeature.entries.sumOf { v[it] * weights[it] } +
            v.namedValues().entries.sumOf { (k,value) -> value * weights.named(k) }
}
