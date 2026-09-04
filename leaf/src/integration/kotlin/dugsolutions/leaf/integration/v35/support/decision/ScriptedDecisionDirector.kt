package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.DecisionDirector

/**
 * One player's queue-driven integration-test decision bundle.
 *
 * Each strategy delegates to the corresponding production baseline whenever a
 * test has not scripted that decision. Create a separate instance per player.
 */
class ScriptedDecisionDirector(
    fallback: DecisionDirector = DecisionDirector.baseline()
) {
    val reward = ScriptedRewardStrategy(fallback.reward)
    val wound = ScriptedWoundStrategy(fallback.wound)
    val placement = ScriptedCreaturePlacementStrategy(fallback.placement)
    val cultivation = ScriptedCultivationStrategy(fallback.cultivation)
    val battle = ScriptedBattleStrategy(fallback.battle)
    val buy = ScriptedBuyStrategy(fallback.buy)
    val support = ScriptedSupportStrategy(fallback.support)
    val effect = ScriptedEffectStrategy(fallback.effect)

    val director: DecisionDirector =
        DecisionDirector(
            reward = reward,
            wound = wound,
            placement = placement,
            cultivation = cultivation,
            battle = battle,
            buy = buy,
            support = support,
            effect = effect
        )

    /**
     * Factory intended for one [GameScenario]/one game. A scripted strategy is
     * stateful because consuming a queued decision is part of the test.
     */
    fun singleGameFactory(): PlayerDecisionFactory =
        PlayerDecisionFactory { director }

    fun assertExhausted() {
        reward.assertExhausted()
        wound.assertExhausted()
        placement.assertExhausted()
        cultivation.assertExhausted()
        battle.assertExhausted()
        buy.assertExhausted()
        support.assertExhausted()
        effect.assertExhausted()
    }
}
