package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter

/** Categorical spending rule applied after a direct Support's factual analysis. */
enum class BattleDirectSupportGate {
    NONE,
    PASSED,
    WORM_REQUIRES_MEANINGFUL_VP_GAIN,
    WATER_REQUIRES_POSITIVE_TRANSITION,
    MULCH_REQUIRES_WIN_FLIPPED
}

/**
 * Shared tactical result for one exact direct Support candidate.
 *
 * [analysis] is always deterministic or expected-before-RNG and never mutates
 * gameplay state. [passesSpendingGate] keeps premium-resource policy separate
 * from factual Battle Swing while [individuallyWorthwhile] is the reusable B10
 * continuation input consumed by later Support orchestration.
 */
data class BattleDirectSupportAnalysis(
    val action: BattleSupportAction,
    val analysis: BattleActionAnalysis<BattleSupportAction>,
    val gate: BattleDirectSupportGate
) {
    val passesSpendingGate: Boolean
        get() = gate == BattleDirectSupportGate.NONE || gate == BattleDirectSupportGate.PASSED

    val individuallyWorthwhile: Boolean
        get() = passesSpendingGate && analysis.tacticalValue > 0.0
}

/**
 * Projects ordinary direct Battle Supports through [BattleActionAnalyzer].
 *
 * Covered here:
 * - Bee and direct Worm placement at their current round-modified values;
 * - Butterfly keep-better expectation on its exact die/row;
 * - Water reroll expectation on its exact die/row, gated by a positive named
 *   transition;
 * - Mulch expected roll and best currently legal placement, gated by
 *   WIN_FLIPPED.
 *
 * Direct Worm placement is gated by meaningful projected Strike-VP gain.
 * Target-dependent special Wisps and the two-step upgrade target belong to B13.
 * Other Wisps therefore retain card-local intrinsic scoring until a complete
 * tactical realization exists; this helper does not invent a target or consume RNG.
 */
class BattleDirectSupportAnalyzer(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val ownTotalChangeAnalyzer: BattleOwnTotalChangeAnalyzer =
        BattleOwnTotalChangeAnalyzer(policy),
    private val placementAnalyzer: BattleDiePlacementAnalyzer =
        BattleDiePlacementAnalyzer(policy)
) {
    operator fun invoke(
        context: DecisionContext,
        action: BattleSupportAction
    ): BattleDirectSupportAnalysis? =
        when (action) {
            is BattleSupportAction.PlaceCritter -> analyzeCritter(context, action)
            is BattleSupportAction.Shared -> analyzeShared(context, action)
        }

    private fun analyzeCritter(
        context: DecisionContext,
        action: BattleSupportAction.PlaceCritter
    ): BattleDirectSupportAnalysis? {
        val value = when (action.critter) {
            Critter.BEE -> context.self.board.beeValue
            Critter.WORM -> context.self.board.wormValue
        }
        val analysis = ownTotalChangeAnalyzer(
            context = context,
            realization = action as BattleSupportAction,
            row = action.row,
            change = value.toDouble(),
            mode = BattleAnalysisMode.DETERMINISTIC
        ) ?: return null
        val gate = if (
            action.critter == Critter.WORM &&
            analysis.vpImpact.gain < policy.battleMinimumMeaningfulVpGain(context)
        ) {
            BattleDirectSupportGate.WORM_REQUIRES_MEANINGFUL_VP_GAIN
        } else {
            BattleDirectSupportGate.NONE
        }
        return BattleDirectSupportAnalysis(action, analysis, gate)
    }

    private fun analyzeShared(
        context: DecisionContext,
        action: BattleSupportAction.Shared
    ): BattleDirectSupportAnalysis? {
        return when (val shared = action.action) {
            is SupportAction.UseButterfly -> {
                val row = findDieRow(context, shared.die.index) ?: return null
                ownTotalChangeAnalyzer(
                    context = context,
                    realization = action as BattleSupportAction,
                    row = row,
                    change = DieValueHeuristics.expectedKeepBestRerollGain(
                        shared.die.sides,
                        shared.die.value
                    ),
                    mode = BattleAnalysisMode.EXPECTED
                )?.let { BattleDirectSupportAnalysis(action, it, BattleDirectSupportGate.NONE) }
            }

            is SupportAction.UseWaterReroll -> {
                val row = findDieRow(context, shared.die.index) ?: return null
                val factual = ownTotalChangeAnalyzer(
                    context = context,
                    realization = action as BattleSupportAction,
                    row = row,
                    change = DieValueHeuristics.expectedRerollGain(
                        shared.die.sides,
                        shared.die.value
                    ),
                    mode = BattleAnalysisMode.EXPECTED
                ) ?: return null
                val analysis = BattleDirectSupportAnalysis(
                    action = action,
                    analysis = factual,
                    gate = if (factual.hasPositiveNamedTransition()) {
                        BattleDirectSupportGate.PASSED
                    } else {
                        BattleDirectSupportGate.WATER_REQUIRES_POSITIVE_TRANSITION
                    }
                )
                analysis
            }

            is SupportAction.UseMulch -> analyzeMulch(context, action, shared)

            is SupportAction.PlayWisp,
            SupportAction.UseWaterRefresh,
            is SupportAction.UseWormFlip -> null
        }
    }

    private fun analyzeMulch(
        context: DecisionContext,
        action: BattleSupportAction.Shared,
        mulch: SupportAction.UseMulch
    ): BattleDirectSupportAnalysis? {
        val sides = mulch.token.sides?.value ?: return null
        val placement = placementAnalyzer(
            context = context,
            dieValue = DieValueHeuristics.expectedRoll(sides),
            mode = BattleAnalysisMode.EXPECTED
        ).maxByOrNull { it.tacticalValue } ?: return null
        val analysis = BattleActionAnalysis<BattleSupportAction>(
            realization = action,
            mode = placement.mode,
            swing = placement.swing,
            vpImpact = placement.vpImpact,
            improvementStepCount = placement.improvementStepCount
        )
        return BattleDirectSupportAnalysis(
            action = action,
            analysis = analysis,
            gate = if (analysis.hasPositiveTransition(BattleTransition.WIN_FLIPPED)) {
                BattleDirectSupportGate.PASSED
            } else {
                BattleDirectSupportGate.MULCH_REQUIRES_WIN_FLIPPED
            }
        )
    }

    private fun findDieRow(context: DecisionContext, handIndex: Int): StrikeRow? =
        context.battle?.rows?.firstOrNull { row ->
            row.forPlayer(context.self.id)?.dice?.any { it.handIndex == handIndex } == true
        }?.row

    private fun BattleActionAnalysis<*>.hasPositiveNamedTransition(): Boolean =
        swing.rowSwings.any { swing ->
            swing.transition != BattleTransition.NONE && swing.transitionBase > 0.0
        }

    private fun BattleActionAnalysis<*>.hasPositiveTransition(
        transition: BattleTransition
    ): Boolean =
        swing.rowSwings.any { swing ->
            swing.transition == transition && swing.transitionBase > 0.0
        }
}
