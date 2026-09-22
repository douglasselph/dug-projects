package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Fresh Step-5 orchestration facts for Human Baseline.
 *
 * This helper deliberately decides only whether continuation is warranted and
 * which legal candidates survive that gate. Final candidate selection remains
 * in [HumanBaselineBattleStrategy] so normal influences and strategy tie-breaking
 * continue to apply in one place.
 */
data class BattleTurnOrchestration(
    val continuation: BattleContinuationAssessment,
    val tempo: BattleTempoAssessment,
    val worthwhileSupports: List<BattleSupportAction>,
    val finalMains: List<BattleMainAction>
) {
    val chooseSupport: Boolean
        get() = continuation.shouldContinue && worthwhileSupports.isNotEmpty()
}

/**
 * Orchestrates Support versus Final Main without reimplementing B10-B12 analysis.
 *
 * Direct and enabling Supports must pass their dedicated resource gates. Other
 * Supports (primarily Wisps) qualify only when their existing card-local score is
 * genuinely positive. If continuation exists only through cumulative ordinary
 * reachability, legal Bees and Butterflies are admitted as the one-at-a-time
 * moves that can advance that path.
 */
class BattleTurnOrchestrator(
    private val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val continuationAssessor: BattleContinuationAssessor = BattleContinuationAssessor(policy),
    private val tempoAssessor: BattleTempoAssessor = BattleTempoAssessor(policy),
    private val directAnalyzer: BattleDirectSupportAnalyzer = BattleDirectSupportAnalyzer(policy),
    private val enablingAnalyzer: BattleEnablingSupportAnalyzer =
        BattleEnablingSupportAnalyzer(cardScorers, policy)
) {
    operator fun invoke(
        context: DecisionContext,
        roundCard: RoundCard,
        legalChoices: Collection<BattleTurnAction>
    ): BattleTurnOrchestration {
        val finals = legalChoices.filterIsInstance<BattleTurnAction.FinalMain>().map { it.action }
        val supports = legalChoices.filterIsInstance<BattleTurnAction.Support>().map { it.action }

        val individuallyWorthwhile = supports.filter { support ->
            individuallyWorthwhile(context, roundCard, support, finals, supports)
        }

        val continuation = continuationAssessor(
            context = context,
            legalChoices = legalChoices,
            individuallyWorthwhileSupportExists = individuallyWorthwhile.isNotEmpty()
        )

        val tempo = tempoAssessor(
            context = context,
            legalChoices = legalChoices,
            finalMains = finals
        )

        val worthwhile = when {
            !continuation.shouldContinue -> emptyList()
            individuallyWorthwhile.isNotEmpty() -> individuallyWorthwhile
            continuation.hasMeaningfulCumulativePath -> supports.filter(::contributesToCumulativePath)
            else -> emptyList()
        }

        return BattleTurnOrchestration(
            continuation = continuation,
            tempo = tempo,
            worthwhileSupports = worthwhile,
            finalMains = finals
        )
    }

    private fun individuallyWorthwhile(
        context: DecisionContext,
        roundCard: RoundCard,
        support: BattleSupportAction,
        currentFinalMains: Collection<BattleMainAction>,
        legalSupports: Collection<BattleSupportAction>
    ): Boolean {
        directAnalyzer(context, support)?.let { return it.individuallyWorthwhile }

        val enabling = (support as? BattleSupportAction.Shared)?.let {
            enablingAnalyzer(
                context = context,
                roundCard = roundCard,
                action = it,
                currentFinalMains = currentFinalMains,
                legalSupports = legalSupports
            )
        }
        if (enabling != null) return enabling.individuallyWorthwhile

        return softSupportScore(context, support).total > 0
    }

    private fun softSupportScore(
        context: DecisionContext,
        support: BattleSupportAction
    ): PriorityScore =
        when (support) {
            is BattleSupportAction.PlaceCritter -> PriorityScore(0)
            is BattleSupportAction.Shared -> when (val shared = support.action) {
                is SupportAction.PlayWisp ->
                    cardScorers.forWisp(shared.card).wispPlayScore(context, shared.card)
                else -> PriorityScore(0)
            }
        }

    private fun contributesToCumulativePath(support: BattleSupportAction): Boolean =
        when (support) {
            is BattleSupportAction.PlaceCritter -> support.critter == Critter.BEE
            is BattleSupportAction.Shared -> support.action is SupportAction.UseButterfly
        }
}
