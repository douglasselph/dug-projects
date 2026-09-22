package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.random.die.DieSides

/** Lexicographic B13 facts for one legal Overgrowth target. */
data class BattleTwoStepUpgradeEvaluation(
    val choice: EffectDieChoice,
    val resultingSides: DieSides,
    val battleAnalysis: BattleActionAnalysis<EffectDieChoice>?
)

/**
 * Overgrowth target policy: maximize the legal resulting die size after two
 * currently available larger steps. Only equivalent resulting sizes are broken
 * by expected current-Battle value. No roll is performed or pre-resolved here.
 */
class BattleTwoStepUpgradeEvaluator(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val ownTotalChangeAnalyzer: BattleOwnTotalChangeAnalyzer =
        BattleOwnTotalChangeAnalyzer(policy)
) {
    operator fun invoke(
        context: DecisionContext,
        choice: EffectDieChoice
    ): BattleTwoStepUpgradeEvaluation? {
        val resulting = resultingSides(context, choice) ?: return null
        val battle = context.battle
        val analysis = if (battle != null) {
            val row = battle.rows.firstOrNull { row ->
                row.forPlayer(context.self.id)?.dice?.any { it.handIndex == choice.index } == true
            }?.row
            row?.let {
                ownTotalChangeAnalyzer(
                    context = context,
                    realization = choice,
                    row = it,
                    change = DieValueHeuristics.expectedRoll(resulting.value) - choice.value,
                    mode = BattleAnalysisMode.EXPECTED
                )
            }
        } else null
        return BattleTwoStepUpgradeEvaluation(choice, resulting, analysis)
    }

    fun evaluateAll(
        context: DecisionContext,
        legalChoices: Collection<EffectDieChoice>
    ): List<BattleTwoStepUpgradeEvaluation> =
        legalChoices.mapNotNull { invoke(context, it) }

    private fun resultingSides(
        context: DecisionContext,
        choice: EffectDieChoice
    ): DieSides? {
        val from = DieSides.from(choice.sides)
        val larger = LADDER.dropWhile { it != from }.drop(1)
        val available = larger.filter { (context.grove.graftBed[it] ?: 0) > 0 }
        return available.getOrNull(1)
    }

    private companion object {
        val LADDER = listOf(
            DieSides.D4,
            DieSides.D6,
            DieSides.D8,
            DieSides.D10,
            DieSides.D12,
            DieSides.D20
        )
    }
}
