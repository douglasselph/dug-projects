package dugsolutions.leaf.v35.player.decision.learned.battle.main

import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleMainPolicy
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleMainActionRequest
import dugsolutions.leaf.v35.player.decision.battle.stableBattleMainId

class LearnedBattleMainPolicy(weights: LearnedBattleMainWeights) : BattleMainPolicy {
    private val scorer = LinearBattleMainScorer(weights)

    override fun chooseMainAction(request: ChooseBattleMainActionRequest): BattleMainAction =
        request.legalActions.maxWithOrNull(
            compareBy<BattleMainAction> { scorer.score(BattleMainFeatureExtractor.extract(request, it)) }
                .thenByDescending { it.stableBattleMainId() }
        ) ?: error("Battle Main learner received no legal action")
}

class LinearBattleMainScorer(private val weights: LearnedBattleMainWeights) {
    fun score(v: BattleMainFeatureVector): Double =
        BattleMainFeature.entries.sumOf { v[it] * weights[it] } +
            v.namedValues().entries.sumOf { (key, value) -> value * weights.named(key) }
}
