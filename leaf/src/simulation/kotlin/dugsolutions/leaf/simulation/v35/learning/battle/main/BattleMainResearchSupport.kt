package dugsolutions.leaf.simulation.v35.learning.battle.main

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.battle.main.LearnedBattleMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.battle.main.LearnedBattleMainWeights
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyStrategy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectPolicy
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectWeights
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayPolicy
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

data class FrozenBattleMainCompanions(
    val buy: LearnedBuyWeights? = null,
    val cultivationMain: LearnedCultivationMainWeights? = null,
    val cultivationSupport: LearnedCultivationSupportWeights? = null,
    val wisp: LearnedWispPlayWeights? = null,
    val plantEffect: LearnedPlantEffectWeights? = null,
    val battleSupport: LearnedBattleSupportWeights? = null
)

internal fun learnedBattleMainFactory(
    weights: LearnedBattleMainWeights,
    companions: FrozenBattleMainCompanions = FrozenBattleMainCompanions()
): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer) = create(strategyRandomizer, DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
        HumanBaselineDecisionDirector(strategyRandomizer = strategyRandomizer, reasoningSink = reasoningSink)
            .createDirector().let { baseline ->
                baseline.copy(
                    buy = companions.buy?.let { LearnedBuyStrategy(it, baseline.buy) } ?: baseline.buy,
                    cultivationMain = companions.cultivationMain?.let { LearnedCultivationMainPolicy(it) } ?: baseline.cultivationMain,
                    cultivationSupport = companions.cultivationSupport?.let { LearnedCultivationSupportPolicy(it) } ?: baseline.cultivationSupport,
                    wispPlay = companions.wisp?.let { LearnedWispPlayPolicy(it) } ?: baseline.wispPlay,
                    plantEffect = companions.plantEffect?.let { LearnedPlantEffectPolicy(it) } ?: baseline.plantEffect,
                    battleSupport = companions.battleSupport?.let { LearnedBattleSupportPolicy(it) } ?: baseline.battleSupport,
                    battleMain = LearnedBattleMainPolicy(weights)
                )
            }
}
