package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleEnabledPlantAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.card.PlantPreservationEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.wound.MechanicalWoundStrategy
import dugsolutions.leaf.v35.player.decision.wound.ChooseWoundRequest
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import dugsolutions.leaf.v35.player.decision.wound.WoundStrategy

class HumanBaselineWoundStrategy(
    private val delegate: WoundStrategy = MechanicalWoundStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers),
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    internal val battleEnabledPlantAnalyzer: BattleEnabledPlantAnalyzer =
        BattleEnabledPlantAnalyzer(cardScorers = cardScorers, policy = policy),
    internal val plantPreservationEvaluator: PlantPreservationEvaluator =
        PlantPreservationEvaluator(cardScorers)
) : WoundStrategy {
    override fun choose(request: ChooseWoundRequest): WoundChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.choose(request)
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = request.legalChoices.map { choice ->
                DecisionCandidate(
                    choice = choice,
                    score = WoundPriority.score(
                        context = request.context,
                        choice = choice,
                        battleEnabledPlantAnalyzer = battleEnabledPlantAnalyzer,
                        plantPreservationEvaluator = plantPreservationEvaluator
                    )
                )
            },
            influenceRegistry = influenceRegistry
        )
    }
}
