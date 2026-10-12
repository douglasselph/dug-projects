package dugsolutions.leaf.v35.player.decision.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.HumanBaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.*
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

/** Replaces ONLY Main policy selection; Support/Buy/Battle continue using their own policies. */
class ConfiguredCultivationMainPolicy(
    private val learned: CultivationMainPolicy,
    private val scoring: HumanScoringConfiguration,
    private val experiment: DecisionExperimentConfiguration,
    randomizer: StrategyRandomizer,
    reasoningSink: DecisionReasoningSink = DecisionReasoningSink.NONE
) : CultivationMainPolicy {
    private val human = HumanBaselineCultivationStrategy(
        strategyRandomizer = randomizer,
        scoreEngine = BaselineScoreEngine(randomizer, reasoningSink),
        modernScoring = scoring
    )

    override fun chooseMainAction(request: ChooseCultivationMainActionRequest): CultivationMainAction {
        val mode = experiment.cultivationMainPolicy
        val compostPresent = request.legalActions.any { action ->
            val effect = when (action) {
                CultivationMainAction.RoundEffect1 -> request.observation.firstRoundEffect
                CultivationMainAction.RoundEffect2 -> request.observation.secondRoundEffect
                else -> null
            }
            effect == GameEffect.UPGRADE_DIE_FROM_HAND ||
                effect == GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS ||
                effect == GameEffect.UPGRADE_DIE_AND_USE_NOW
        }
        if (mode == CultivationMainPolicyMode.LEARNED ||
            (mode == CultivationMainPolicyMode.HUMAN_WHEN_COMPOST_AVAILABLE && !compostPresent)
        ) return learned.chooseMainAction(request)
        if (mode == CultivationMainPolicyMode.LEGACY_HUMAN_BASELINE) return request.referenceAction
        // A fresh RoundCard isn't needed: the effective one supplied to the strategy
        // is supplied by the coordinator in the main-policy request.
        val original = request.observation.roundCard
            ?: error("Scored Human Main policy requires effective round card")
        val chosen = human.chooseAction(ChooseCultivationActionRequest(
            roundCard = original,
            mainActionsRemaining = request.observation.mainActionsRemaining,
            legalChoices = request.legalActions.map { CultivationAction.Main(it) },
            context = request.observation.context
        ))
        return (chosen as CultivationAction.Main).action
    }
}
