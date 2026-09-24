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
 * extension examples, the Cultivation decisions that introduced this layer, and
 * the Battle Stage-A thresholds added during Battle B1.
 */
open class HumanBaselinePolicy(
    private val protectedBeeReserveValue: Int = DEFAULT_PROTECTED_BEE_RESERVE,
    private val protectedWormReserveValue: Int = DEFAULT_PROTECTED_WORM_RESERVE,
    private val protectedWaterReserveValue: Int = DEFAULT_PROTECTED_WATER_RESERVE,
    private val protectedMulchReserveValue: Int = DEFAULT_PROTECTED_MULCH_RESERVE,
    private val postReserveBeeProbabilityValue: Double = DEFAULT_POST_RESERVE_BEE_PROBABILITY,
    private val graftAdequateGrowthSlotsValue: Int = DEFAULT_GRAFT_ADEQUATE_GROWTH_SLOTS,
    private val cultivationDiceDeficitPointsPerPowerValue: Int =
        DEFAULT_CULTIVATION_DICE_DEFICIT_POINTS_PER_POWER,
    private val cultivationDiceDeficitMaxBonusValue: Int =
        DEFAULT_CULTIVATION_DICE_DEFICIT_MAX_BONUS,
    private val cultivationDoneScoreValue: Int = DEFAULT_CULTIVATION_DONE_SCORE,
    private val cultivationReserveSpendPenaltyPerUnitValue: Int =
        DEFAULT_CULTIVATION_RESERVE_SPEND_PENALTY_PER_UNIT,
    private val battleTransitionScaleValue: Int = DEFAULT_BATTLE_TRANSITION_SCALE,
    private val battleCloseMarginValue: Int = DEFAULT_BATTLE_CLOSE_MARGIN,
    private val battleSecuredLeadValue: Int = DEFAULT_BATTLE_SECURED_LEAD,
    private val battleHopelessDeficitValue: Int = DEFAULT_BATTLE_HOPELESS_DEFICIT,
    private val battleMinimumMeaningfulVpGainValue: Int =
        DEFAULT_BATTLE_MINIMUM_MEANINGFUL_VP_GAIN,
    private val battleWaterRefreshMinImprovementStepsValue: Int =
        DEFAULT_BATTLE_WATER_REFRESH_MIN_IMPROVEMENT_STEPS,
    private val battleImmediateResolveMinLeadValue: Int =
        DEFAULT_BATTLE_IMMEDIATE_RESOLVE_MIN_LEAD,
    private val battleImmediateResolveMinOpponentSupportValue: Int =
        DEFAULT_BATTLE_IMMEDIATE_RESOLVE_MIN_OPPONENT_SUPPORT,
    private val developmentTargetConfig: DevelopmentTargetConfig = DevelopmentTargetConfig()
) {
    init {
        require(protectedBeeReserveValue >= 0) { "Protected Bee reserve cannot be negative" }
        require(protectedWormReserveValue >= 0) { "Protected Worm reserve cannot be negative" }
        require(protectedWaterReserveValue >= 0) { "Protected Water reserve cannot be negative" }
        require(protectedMulchReserveValue >= 0) { "Protected Mulch reserve cannot be negative" }
        require(postReserveBeeProbabilityValue in 0.0..1.0) {
            "Post-reserve Bee probability must be between 0.0 and 1.0"
        }
        require(graftAdequateGrowthSlotsValue >= 2) {
            "Adequate Graft growth-slot threshold must be at least 2"
        }
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
        require(battleTransitionScaleValue > 0) { "Battle transition scale must be positive" }
        require(battleCloseMarginValue >= 0) { "Battle close margin cannot be negative" }
        require(battleSecuredLeadValue >= 0) { "Battle secured lead cannot be negative" }
        require(battleHopelessDeficitValue >= 0) { "Battle hopeless deficit cannot be negative" }
        require(battleMinimumMeaningfulVpGainValue >= 0) {
            "Battle minimum meaningful VP gain cannot be negative"
        }
        require(battleWaterRefreshMinImprovementStepsValue >= 0) {
            "Battle Water-refresh improvement-step threshold cannot be negative"
        }
        require(battleImmediateResolveMinLeadValue >= 0) {
            "Battle immediate-resolve lead cannot be negative"
        }
        require(battleImmediateResolveMinOpponentSupportValue >= 0) {
            "Battle immediate-resolve opponent Support threshold cannot be negative"
        }
    }

    companion object {
        /** Normal Critter reserve Human Baseline tries to carry toward Battle. */
        const val DEFAULT_PROTECTED_BEE_RESERVE: Int = 2
        const val DEFAULT_PROTECTED_WORM_RESERVE: Int = 1
        const val DEFAULT_PROTECTED_WATER_RESERVE: Int = 1
        const val DEFAULT_PROTECTED_MULCH_RESERVE: Int = 1

        /** Neutral Critter rewards favor Bees two times out of three after the reserve is filled. */
        const val DEFAULT_POST_RESERVE_BEE_PROBABILITY: Double = 2.0 / 3.0

        /** Future Vine-growth slots at which Graft Placement considers expansion room adequate. */
        const val DEFAULT_GRAFT_ADEQUATE_GROWTH_SLOTS: Int = 3

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

        /** Base spacing between Human Baseline Battle transition tiers. */
        const val DEFAULT_BATTLE_TRANSITION_SCALE: Int = 100

        /** Live-threat margin at or inside which a Strike Row is a close contest. */
        const val DEFAULT_BATTLE_CLOSE_MARGIN: Int = 4

        /** Live-threat lead at which Human Baseline treats a winning row as secured for now. */
        const val DEFAULT_BATTLE_SECURED_LEAD: Int = 10

        /** Score-benchmark deficit at which a non-winning row becomes potentially hopeless. */
        const val DEFAULT_BATTLE_HOPELESS_DEFICIT: Int = 10

        /** Minimum Strike-VP improvement that normally justifies a substantial Support commitment. */
        const val DEFAULT_BATTLE_MINIMUM_MEANINGFUL_VP_GAIN: Int = 2

        /** Minimum named-transition steps that normally justify spending Water to refresh. */
        const val DEFAULT_BATTLE_WATER_REFRESH_MIN_IMPROVEMENT_STEPS: Int = 2

        /** Minimum lead over every opponent before the immediate-resolve Wisp is considered. */
        const val DEFAULT_BATTLE_IMMEDIATE_RESOLVE_MIN_LEAD: Int = 5

        /** Minimum remaining Support moves on each live contender before immediate resolution is attractive. */
        const val DEFAULT_BATTLE_IMMEDIATE_RESOLVE_MIN_OPPONENT_SUPPORT: Int = 3
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
     * Bee probability for an otherwise-neutral Critter reward after both
     * protected Critter minimums have been established.
     *
     * This is strategy variation, not game randomness. Reward strategy code
     * consumes StrategyRandomizer when applying it.
     */
    open fun postReserveBeeProbability(context: DecisionContext): Double =
        postReserveBeeProbabilityValue

    /**
     * Minimum future Vine-growth slots that Graft Placement treats as ADEQUATE.
     * Lower positive counts are CONSTRAINED; zero is BOXED IN.
     */
    open fun graftAdequateGrowthSlots(context: DecisionContext): Int =
        graftAdequateGrowthSlotsValue

    /**
     * Resources Human Baseline normally prefers to preserve during Cultivation.
     *
     * Critter values delegate to [protectedCritterReserve] so the canonical
     * 2-Bee/1-Worm assumption remains defined once. Water and Mulch begin with
     * one protected unit each. Experiments may override this method directly.
     */
    open fun protectedCultivationResourceReserve(context: DecisionContext): ResourceReserveTargets {
        val critters = protectedCritterReserve(context)
        return ResourceReserveTargets(
            bees = critters.bees,
            worms = critters.worms,
            water = protectedWaterReserveValue,
            mulch = protectedMulchReserveValue
        )
    }

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

    /** Normal starting dice-pool power used by coarse visible-development comparisons. */
    open fun startingDicePower(): Int = developmentTargetConfig.startingDicePower

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

    /** Base spacing used when Battle Swing assigns importance to named row transitions. */
    open fun battleTransitionScale(context: DecisionContext): Int =
        battleTransitionScaleValue

    /** Absolute Live-Threat margin that Human Baseline treats as a close Battle contest. */
    open fun battleCloseMargin(context: DecisionContext): Int =
        battleCloseMarginValue

    /** Live-Threat lead at which a currently winning row is treated as secured for this decision. */
    open fun battleSecuredLead(context: DecisionContext): Int =
        battleSecuredLeadValue

    /** Score-Benchmark deficit at which a non-winning row gets only exceptional tactical attention. */
    open fun battleHopelessDeficit(context: DecisionContext): Int =
        battleHopelessDeficitValue

    /** Minimum immediate Strike-VP improvement that normally justifies a large Support commitment. */
    open fun battleMinimumMeaningfulVpGain(context: DecisionContext): Int =
        battleMinimumMeaningfulVpGainValue

    /** Minimum named Battle-improvement steps that normally justify Water Refresh. */
    open fun battleWaterRefreshMinImprovementSteps(context: DecisionContext): Int =
        battleWaterRefreshMinImprovementStepsValue

    /** Minimum lead over every participating opponent for the immediate-Strike-resolution Wisp. */
    open fun battleImmediateResolveMinLead(context: DecisionContext): Int =
        battleImmediateResolveMinLeadValue

    /** Minimum Support capacity on each live contender that makes locking a won row attractive. */
    open fun battleImmediateResolveMinOpponentSupport(context: DecisionContext): Int =
        battleImmediateResolveMinOpponentSupportValue

}

