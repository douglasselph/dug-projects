package dugsolutions.leaf.simulation.v35.learning.cultivation

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyStrategy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry

internal fun learnedCultivationFactory(
    cultivationWeights: LearnedCultivationMainWeights,
    buyWeights: LearnedBuyWeights? = null
): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create())

    override fun create(strategyRandomizer: StrategyRandomizer) =
        create(strategyRandomizer, DecisionReasoningSink.NONE)

    override fun create(
        strategyRandomizer: StrategyRandomizer,
        reasoningSink: DecisionReasoningSink
    ) = HumanBaselineDecisionDirector(
        strategyRandomizer = strategyRandomizer,
        reasoningSink = reasoningSink
    ).createDirector().let { baseline ->
        var director = baseline.copy(
            cultivationMain = LearnedCultivationMainPolicy(cultivationWeights)
        )
        if (buyWeights != null) {
            director = director.copy(buy = LearnedBuyStrategy(buyWeights, baseline.buy))
        }
        director
    }
}

internal fun humanCultivationFactory(
    buyWeights: LearnedBuyWeights? = null
): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create())

    override fun create(strategyRandomizer: StrategyRandomizer) =
        create(strategyRandomizer, DecisionReasoningSink.NONE)

    override fun create(
        strategyRandomizer: StrategyRandomizer,
        reasoningSink: DecisionReasoningSink
    ) = HumanBaselineDecisionDirector(
        strategyRandomizer = strategyRandomizer,
        reasoningSink = reasoningSink
    ).createDirector().let { baseline ->
        if (buyWeights == null) baseline
        else baseline.copy(buy = LearnedBuyStrategy(buyWeights, baseline.buy))
    }
}

internal fun loadCultivationResearchCards(
    plantRegistry: PlantCardRegistry,
    plantManager: PlantCardManager,
    wispRegistry: WispCardRegistry,
    wispManager: WispCardManager,
    roundRegistry: RoundCardRegistry,
    roundManager: RoundCardManager
) {
    val root = CardDataFiles.dataDirectory()
    plantRegistry.clear()
    plantRegistry.loadFromCsv(
        CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root),
        CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root)
    )
    plantManager.loadCards(plantRegistry)
    wispRegistry.clear()
    wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
    wispManager.loadCards(wispRegistry)
    roundRegistry.clear()
    roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
    roundManager.loadCards(roundRegistry)
}

internal fun affectedSeat(sample: Int, players: Int): Int {
    require(sample >= 0)
    require(players in 2..4)
    return sample % players
}
