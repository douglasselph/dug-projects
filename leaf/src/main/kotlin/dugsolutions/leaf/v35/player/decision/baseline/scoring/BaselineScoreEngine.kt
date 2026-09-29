package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoning
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningAdjustment
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningAlternative
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

/**
 * Selects the highest-scoring Human Baseline candidate.
 *
 * The strategy RNG is consumed only when two or more candidates share the
 * highest total. Mechanical game randomness is never accepted by this class.
 * When a reasoning sink is enabled, the selected choice and every legal scored
 * alternative are exposed as typed diagnostic data. Normal large simulations
 * use DecisionReasoningSink.NONE and do not construct those snapshots.
 */
class BaselineScoreEngine(
    private val randomizer: StrategyRandomizer = StrategyRandomizer.create(),
    private val reasoningSink: DecisionReasoningSink = DecisionReasoningSink.NONE
) {
    fun <T> choose(
        choices: List<ScoredChoice<T>>
    ): ScoredChoice<T> {
        require(choices.isNotEmpty()) {
            "Cannot choose from an empty scored-choice list"
        }

        val highest = choices.maxOf { it.score.total }
        val tied = choices.filter { it.score.total == highest }

        val selected =
            if (tied.size == 1) {
                tied.single()
            } else {
                tied[randomizer.nextInt(tied.size)]
            }

        if (reasoningSink !== DecisionReasoningSink.NONE) {
            reasoningSink.record(
                selected.toDecisionReasoning(
                    alternatives = choices.map { choice ->
                        choice.toDecisionReasoningAlternative(
                            selected = choice === selected
                        )
                    }
                )
            )
        }
        return selected
    }

    fun <T> chooseValue(
        choices: List<ScoredChoice<T>>
    ): T = choose(choices).choice

    /** Score every legal candidate, including all owned-card influences. */
    fun <T> scoreCandidates(
        context: DecisionContext,
        candidates: List<DecisionCandidate<T>>,
        influenceRegistry: BaselineInfluenceRegistry
    ): List<ScoredChoice<T>> {
        require(candidates.isNotEmpty()) {
            "Cannot score an empty Human Baseline candidate list"
        }
        return candidates.map { candidate ->
            influenceRegistry.score(context, candidate)
        }
    }

    /** Score all legal candidates with shared influences, then select the best. */
    fun <T> choose(
        context: DecisionContext,
        candidates: List<DecisionCandidate<T>>,
        influenceRegistry: BaselineInfluenceRegistry
    ): ScoredChoice<T> =
        choose(scoreCandidates(context, candidates, influenceRegistry))

    fun <T> chooseValue(
        context: DecisionContext,
        candidates: List<DecisionCandidate<T>>,
        influenceRegistry: BaselineInfluenceRegistry
    ): T = choose(context, candidates, influenceRegistry).choice

    private fun <T> ScoredChoice<T>.toDecisionReasoning(
        alternatives: List<DecisionReasoningAlternative>
    ): DecisionReasoning =
        DecisionReasoning(
            choiceLabel = label,
            baseScore = score.base,
            adjustments = score.adjustments.map { adjustment ->
                DecisionReasoningAdjustment(
                    amount = adjustment.amount,
                    reason = adjustment.reason
                )
            },
            total = score.total,
            observations = observations,
            alternatives = alternatives
        )

    private fun <T> ScoredChoice<T>.toDecisionReasoningAlternative(
        selected: Boolean
    ): DecisionReasoningAlternative =
        DecisionReasoningAlternative(
            choiceLabel = label,
            baseScore = score.base,
            adjustments = score.adjustments.map { adjustment ->
                DecisionReasoningAdjustment(
                    amount = adjustment.amount,
                    reason = adjustment.reason
                )
            },
            total = score.total,
            observations = observations,
            selected = selected
        )
}
