package dugsolutions.leaf.simulation.v35.experiment.decisioncalibration

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

/** Research-only one-shot intervention used by targeted counterfactual calibration. */
enum class CounterfactualBranch { USE_EFFECT, DRAW }

data class CounterfactualIntervention(
    var opportunitySeen: Boolean = false,
    var baselineChoice: String? = null,
    var forcedChoice: String? = null
)

fun counterfactualHumanBaselineFactory(
    cardName: String,
    branch: CounterfactualBranch,
    intervention: CounterfactualIntervention
): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create(): DecisionDirector = create(StrategyRandomizer.create(), DecisionReasoningSink.NONE)

    override fun create(strategyRandomizer: StrategyRandomizer): DecisionDirector =
        create(strategyRandomizer, DecisionReasoningSink.NONE)

    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink): DecisionDirector {
        val baseline = DecisionDirector.humanBaseline(strategyRandomizer, reasoningSink)
        val wrapped = OneShotCultivationCounterfactual(
            delegate = baseline.cultivation,
            cardName = cardName,
            branch = branch,
            intervention = intervention
        )
        return baseline.copy(cultivation = wrapped)
    }
}

private class OneShotCultivationCounterfactual(
    private val delegate: CultivationStrategy,
    private val cardName: String,
    private val branch: CounterfactualBranch,
    private val intervention: CounterfactualIntervention
) : CultivationStrategy {
    private var consumed = false

    override fun chooseAction(request: ChooseCultivationActionRequest): CultivationAction {
        // Always ask Human Baseline first. Both paired branches therefore consume the same
        // strategy-RNG calls at the intervention decision before either result is overridden.
        val baselineChoice = delegate.chooseAction(request)
        if (consumed) return baselineChoice

        val plantChoice = request.legalChoices.filterIsInstance<CultivationAction.Main>()
            .firstOrNull { main ->
                val action = main.action
                action is CultivationMainAction.ActivatePlant && action.card.card.name.equals(cardName, ignoreCase = true)
            }
        val drawChoice = request.legalChoices.filterIsInstance<CultivationAction.Main>()
            .firstOrNull { it.action == CultivationMainAction.Draw }

        if (plantChoice == null || drawChoice == null) return baselineChoice

        consumed = true
        intervention.opportunitySeen = true
        intervention.baselineChoice = baselineChoice.toString()
        val forced = when (branch) {
            CounterfactualBranch.USE_EFFECT -> plantChoice
            CounterfactualBranch.DRAW -> drawChoice
        }
        intervention.forcedChoice = forced.toString()
        return forced
    }
}
