package dugsolutions.leaf.v35.player.decision.baseline

import dugsolutions.leaf.v35.player.decision.baseline.common.DevelopmentTargetConfig
import dugsolutions.leaf.v35.player.decision.baseline.common.DevelopmentTargetHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.ReserveResource
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveTargets
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Shared, experiment-friendly tuning policy for cross-cutting Human Baseline behavior.
 *
 * The companion constants are the canonical defaults. Production strategies should
 * not read those constants directly; they should call the overridable methods on the
 * injected policy instead. This creates three layers:
 *
 * `default constant -> overridable policy method -> strategy/scorer`
 *
 * Simple experiments can construct a policy with different constructor values.
 * Context-sensitive experiments can subclass this type and override one or more
 * methods. The same policy instance is wired through [HumanBaselineDecisionDirector].
 *
 * Keep this class limited to cross-cutting assumptions. Card-specific and
 * action-specific valuation constants belong beside the scorer they describe.
 *
 * See `doc/HUMAN_BASELINE_POLICY.md` for the score scale, current defaults,
 * extension examples, and the Cultivation A3 decisions that introduced this layer.
 */
open class HumanBaselinePolicy(
    private val protectedBeeReserveValue: Int = DEFAULT_PROTECTED_BEE_RESERVE,
    private val protectedWormReserveValue: Int = DEFAULT_PROTECTED_WORM_RESERVE,
    private val cultivationDiceDeficitPointsPerPowerValue: Int =
        DEFAULT_CULTIVATION_DICE_DEFICIT_POINTS_PER_POWER,
    private val cultivationDiceDeficitMaxBonusValue: Int =
        DEFAULT_CULTIVATION_DICE_DEFICIT_MAX_BONUS,
    private val cultivationDoneScoreValue: Int = DEFAULT_CULTIVATION_DONE_SCORE,
    private val cultivationReserveSpendPenaltyPerUnitValue: Int =
        DEFAULT_CULTIVATION_RESERVE_SPEND_PENALTY_PER_UNIT,
    private val developmentTargetConfig: DevelopmentTargetConfig = DevelopmentTargetConfig()
) {
    init {
        require(protectedBeeReserveValue >= 0) { "Protected Bee reserve cannot be negative" }
        require(protectedWormReserveValue >= 0) { "Protected Worm reserve cannot be negative" }
        require(cultivationDiceDeficitPointsPerPowerValue >= 0) {
            "Cultivation dice-deficit points cannot be negative"
        }
        require(cultivationDiceDeficitMaxBonusValue >= 0) {
            "Cultivation dice-deficit max bonus cannot be negative"
        }
        require(cultivationDoneScoreValue >= 0) { "Cultivation Done score cannot be negative" }
        require(cultivationReserveSpendPenaltyPerUnitValue >= 0) {
            "Cultivation reserve-spend penalty cannot be negative"
        }
    }

    companion object {
        /** Normal Critter reserve Human Baseline tries to carry toward Battle. */
        const val DEFAULT_PROTECTED_BEE_RESERVE: Int = 2
        const val DEFAULT_PROTECTED_WORM_RESERVE: Int = 1

        /**
         * Development need is intentionally only a modest nudge. One missing
         * die-power point adds one PriorityScore point, capped at +9.
         */
        const val DEFAULT_CULTIVATION_DICE_DEFICIT_POINTS_PER_POWER: Int = 1
        const val DEFAULT_CULTIVATION_DICE_DEFICIT_MAX_BONUS: Int = 9

        /** Benchmark that remaining Support choices must beat after Main Actions. */
        const val DEFAULT_CULTIVATION_DONE_SCORE: Int = 55

        /**
         * Initial cost per protected resource unit spent during Cultivation.
         * The support rewrite will apply this knob once action-specific Support
         * scoring and the associated reserve targets are finalized.
         */
        const val DEFAULT_CULTIVATION_RESERVE_SPEND_PENALTY_PER_UNIT: Int = 15
    }

    /**
     * Critters normally protected from ordinary purchasing power.
     *
     * The context parameter is intentionally part of the contract even though
     * the default is constant: simulations may override this by round, phase,
     * player state, or any other visible decision context.
     */
    open fun protectedCritterReserve(context: DecisionContext): ResourceReserveTargets =
        ResourceReserveTargets(
            bees = protectedBeeReserveValue,
            worms = protectedWormReserveValue
        )

    /**
     * Purchasing power Human Baseline should normally perceive as spendable:
     * Hand dice plus Critters above the protected reserve.
     *
     * Premium Buy exceptions such as the D20/cost-17 Flower are deliberately
     * excluded; those remain exceptional Buy decisions rather than normal power.
     */
    open fun normalPurchasingPower(context: DecisionContext): Int {
        val board = context.self.board
        val reserve = protectedCritterReserve(context)
        val spendableBees = (board.bees - reserve.bees).coerceAtLeast(0)
        val spendableWorms = (board.worms - reserve.worms).coerceAtLeast(0)
        return board.hand.sumOf { it.value } +
            spendableBees * board.beeValue +
            spendableWorms * board.wormValue
    }

    /**
     * Modest Cultivation bonus for being behind the expected dice-development
     * curve. Strong immediate opportunities should still be able to dominate it.
     */
    open fun cultivationDiceDevelopmentBonus(context: DecisionContext): Int {
        val deficit = DevelopmentTargetHeuristics.assess(
            context = context,
            config = developmentTargetConfig
        ).dicePowerDeficit
        return minOf(
            cultivationDiceDeficitMaxBonusValue,
            deficit * cultivationDiceDeficitPointsPerPowerValue
        )
    }

    /** Score assigned to `Done` after the two required Main Actions are complete. */
    open fun cultivationDoneScore(context: DecisionContext): Int =
        cultivationDoneScoreValue

    /**
     * Score penalty per protected resource unit consumed by a Cultivation
     * Support action. The resource argument makes future per-resource overrides
     * possible without changing scorer APIs.
     */
    open fun cultivationReserveSpendPenaltyPerUnit(
        context: DecisionContext,
        resource: ReserveResource
    ): Int = cultivationReserveSpendPenaltyPerUnitValue
}
