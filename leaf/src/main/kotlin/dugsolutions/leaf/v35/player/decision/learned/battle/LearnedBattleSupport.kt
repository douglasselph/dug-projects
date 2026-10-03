package dugsolutions.leaf.v35.player.decision.learned.battle

import dugsolutions.leaf.v35.player.decision.DecisionDirector

object LearnedBattleSupport {
    fun install(baseline: DecisionDirector, weights: LearnedBattleSupportWeights): DecisionDirector =
        baseline.copy(battleSupport = LearnedBattleSupportPolicy(weights))
}
