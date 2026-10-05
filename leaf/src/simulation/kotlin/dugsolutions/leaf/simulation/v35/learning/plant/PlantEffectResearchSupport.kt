package dugsolutions.leaf.simulation.v35.learning.plant

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselineDecisionDirector
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectPolicy
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectWeights
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

internal fun learnedPlantEffectFactory(weights: LearnedPlantEffectWeights): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer) = create(strategyRandomizer, DecisionReasoningSink.NONE)
    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
        HumanBaselineDecisionDirector(strategyRandomizer=strategyRandomizer, reasoningSink=reasoningSink).createDirector().let { baseline ->
            baseline.copy(plantEffect=LearnedPlantEffectPolicy(weights))
        }
}

/** Effective Plant definitions used only for learned-policy schema provenance. */
internal fun effectivePlantEffectCatalogCards(
    cards: Collection<dugsolutions.leaf.v35.plant.domain.PlantCard>,
    values: dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentConfig
): List<dugsolutions.leaf.v35.plant.domain.PlantCard> =
    cards.map { card ->
        card.copy(
            type = values.typeFor(card),
            cost = values.costFor(card),
            effect = values.effectFor(card),
            scoringRule = values.scoringRuleFor(card)
        )
    }
