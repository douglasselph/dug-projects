package dugsolutions.leaf.v35.player.decision.baseline.reward

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.reward.MechanicalRewardStrategy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Ordinary-human Critter acquisition.
 *
 * Human Baseline first fills the policy-controlled protected Critter minimum.
 * Once both minimums are filled, an otherwise-neutral reward uses the policy's
 * Bee probability through StrategyRandomizer. Visible acquisition influences
 * are then free to override that ordinary preference through normal scoring.
 */
class HumanBaselineRewardStrategy(
    private val delegate: RewardStrategy = MechanicalRewardStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(),
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create()
) : RewardStrategy {
    override fun chooseCritter(request: ChooseCritterRequest): Critter {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseCritter(request)
        if (request.legalChoices.size == 1) return request.legalChoices.single()

        val ordinaryPreference = ordinaryPreference(request.context, request.legalChoices)
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = request.legalChoices.map { choice ->
                DecisionCandidate(
                    choice = choice,
                    score = CritterRewardPriority.score(choice, ordinaryPreference),
                    tags = CritterRewardPriority.tags(choice)
                )
            },
            influenceRegistry = influenceRegistry
        )
    }

    private fun ordinaryPreference(
        context: DecisionContext,
        legalChoices: List<Critter>
    ): Critter {
        val board = context.self.board
        val reserve = policy.protectedCritterReserve(context)
        val beeDeficit = (reserve.bees - board.bees).coerceAtLeast(0)
        val wormDeficit = (reserve.worms - board.worms).coerceAtLeast(0)

        val preferred = when {
            beeDeficit > 0 || wormDeficit > 0 ->
                if (beeDeficit >= wormDeficit) Critter.BEE else Critter.WORM
            else -> {
                val beeProbability = policy.postReserveBeeProbability(context)
                if (strategyRandomizer.nextInt(PROBABILITY_RESOLUTION) <
                    (beeProbability * PROBABILITY_RESOLUTION).toInt()
                ) {
                    Critter.BEE
                } else {
                    Critter.WORM
                }
            }
        }

        return if (preferred in legalChoices) preferred else legalChoices.first()
    }

    private companion object {
        const val PROBABILITY_RESOLUTION: Int = 1_000_000
    }
}
