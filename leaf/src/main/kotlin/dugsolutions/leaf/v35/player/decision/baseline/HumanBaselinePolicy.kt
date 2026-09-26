package dugsolutions.leaf.v35.player.decision.baseline

import dugsolutions.leaf.v35.player.decision.baseline.common.DevelopmentTargetConfig
import dugsolutions.leaf.v35.player.decision.baseline.common.DevelopmentTargetHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.ReserveResource
import dugsolutions.leaf.v35.player.decision.baseline.common.ResourceReserveTargets
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

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
    private val overgrowthD4UsePercentageValue: Int = DEFAULT_OVERGROWTH_D4_USE_PERCENTAGE,
    private val overgrowthD6UsePercentageValue: Int = DEFAULT_OVERGROWTH_D6_USE_PERCENTAGE,
    private val overgrowthD8UsePercentageValue: Int = DEFAULT_OVERGROWTH_D8_USE_PERCENTAGE,
    private val overgrowthD10UsePercentageValue: Int = DEFAULT_OVERGROWTH_D10_USE_PERCENTAGE,
    private val sunlightUsePercentageValue: Int = DEFAULT_SUNLIGHT_USE_PERCENTAGE,
    private val mulchValue1UsePercentageValue: Int = DEFAULT_MULCH_VALUE_1_USE_PERCENTAGE,
    private val mulchValue2UsePercentageValue: Int = DEFAULT_MULCH_VALUE_2_USE_PERCENTAGE,
    private val mulchValue3UsePercentageValue: Int = DEFAULT_MULCH_VALUE_3_USE_PERCENTAGE,
    private val mulchValue4UsePercentageValue: Int = DEFAULT_MULCH_VALUE_4_USE_PERCENTAGE,
    private val pocketedSparkCultivationD4UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_CULTIVATION_D4_USE_PERCENTAGE,
    private val pocketedSparkCultivationD6UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_CULTIVATION_D6_USE_PERCENTAGE,
    private val pocketedSparkCultivationD8UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_CULTIVATION_D8_USE_PERCENTAGE,
    private val pocketedSparkCultivationD10UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_CULTIVATION_D10_USE_PERCENTAGE,
    private val pocketedSparkCultivationD12UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_CULTIVATION_D12_USE_PERCENTAGE,
    private val pocketedSparkCultivationD20UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_CULTIVATION_D20_USE_PERCENTAGE,
    private val pocketedSparkBattleD4UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_BATTLE_D4_USE_PERCENTAGE,
    private val pocketedSparkBattleD6UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_BATTLE_D6_USE_PERCENTAGE,
    private val pocketedSparkBattleD8UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_BATTLE_D8_USE_PERCENTAGE,
    private val pocketedSparkBattleD10UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_BATTLE_D10_USE_PERCENTAGE,
    private val pocketedSparkBattleD12UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_BATTLE_D12_USE_PERCENTAGE,
    private val pocketedSparkBattleD20UsePercentageValue: Int =
        DEFAULT_POCKETED_SPARK_BATTLE_D20_USE_PERCENTAGE,
    private val lowPlantPriorityPercentageValue: Int =
        DEFAULT_LOW_PLANT_PRIORITY_PERCENTAGE,
    private val lowPlantFloorValue: Int =
        DEFAULT_LOW_PLANT_FLOOR,
    private val buyPlantEquivalentDicePowerPerCardValue: Int =
        DEFAULT_BUY_PLANT_EQUIVALENT_DICE_POWER_PER_CARD,
    private val buyBalanceDifferenceFor85PercentValue: Double =
        DEFAULT_BUY_BALANCE_DIFFERENCE_FOR_85_PERCENT,
    private val buyPlantCostTierExponentialBaseValue: Double =
        DEFAULT_BUY_PLANT_COST_TIER_EXPONENTIAL_BASE,
    private val buyCheaperPlantMinimumRemainingDiceValue: Int =
        DEFAULT_BUY_CHEAPER_PLANT_MINIMUM_REMAINING_DICE,
    private val buyBeeOddsMultiplierPerAdditionalBeeValue: Double =
        DEFAULT_BUY_BEE_ODDS_MULTIPLIER_PER_ADDITIONAL_BEE,
    private val buyDieBeeD4BaseOddsValue: Double =
        DEFAULT_BUY_DIE_BEE_D4_BASE_ODDS,
    private val buyDieBeeOddsGrowthPerTierValue: Double =
        DEFAULT_BUY_DIE_BEE_ODDS_GROWTH_PER_TIER,
    private val buyPlantBeeBaseOddsValue: Double =
        DEFAULT_BUY_PLANT_BEE_BASE_ODDS,
    private val buyWormBaseOddsAtTwoWormsValue: Double =
        DEFAULT_BUY_WORM_BASE_ODDS_AT_TWO_WORMS,
    private val buyWormOddsMultiplierPerAdditionalWormValue: Double =
        DEFAULT_BUY_WORM_ODDS_MULTIPLIER_PER_ADDITIONAL_WORM,
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
        require(overgrowthD4UsePercentageValue in 0..100) { "Overgrowth D4 use percentage must be 0..100" }
        require(overgrowthD6UsePercentageValue in 0..100) { "Overgrowth D6 use percentage must be 0..100" }
        require(overgrowthD8UsePercentageValue in 0..100) { "Overgrowth D8 use percentage must be 0..100" }
        require(overgrowthD10UsePercentageValue in 0..100) { "Overgrowth D10 use percentage must be 0..100" }
        require(sunlightUsePercentageValue in 0..100) { "Sunlight use percentage must be 0..100" }
        require(mulchValue1UsePercentageValue in 0..100) { "Mulch value-1 use percentage must be 0..100" }
        require(mulchValue2UsePercentageValue in 0..100) { "Mulch value-2 use percentage must be 0..100" }
        require(mulchValue3UsePercentageValue in 0..100) { "Mulch value-3 use percentage must be 0..100" }
        require(mulchValue4UsePercentageValue in 0..100) { "Mulch value-4 use percentage must be 0..100" }
        listOf(
            pocketedSparkCultivationD4UsePercentageValue,
            pocketedSparkCultivationD6UsePercentageValue,
            pocketedSparkCultivationD8UsePercentageValue,
            pocketedSparkCultivationD10UsePercentageValue,
            pocketedSparkCultivationD12UsePercentageValue,
            pocketedSparkCultivationD20UsePercentageValue,
            pocketedSparkBattleD4UsePercentageValue,
            pocketedSparkBattleD6UsePercentageValue,
            pocketedSparkBattleD8UsePercentageValue,
            pocketedSparkBattleD10UsePercentageValue,
            pocketedSparkBattleD12UsePercentageValue,
            pocketedSparkBattleD20UsePercentageValue,
            lowPlantPriorityPercentageValue
        ).forEach { percentage ->
            require(percentage in 0..100) { "Human Baseline percentage must be 0..100: $percentage" }
        }
        require(lowPlantFloorValue >= 0) { "Low-Plant floor cannot be negative" }
        require(buyPlantEquivalentDicePowerPerCardValue > 0) {
            "Buy Plant-equivalent dice power per card must be positive"
        }
        require(buyBalanceDifferenceFor85PercentValue > 0.0) {
            "Buy balance 85-percent difference must be positive"
        }
        require(buyPlantCostTierExponentialBaseValue > 1.0) {
            "Buy Plant cost-tier exponential base must be greater than 1"
        }
        require(buyCheaperPlantMinimumRemainingDiceValue >= 0) {
            "Buy cheaper-Plant remaining-dice minimum cannot be negative"
        }
        require(buyBeeOddsMultiplierPerAdditionalBeeValue > 0.0) {
            "Buy Bee odds multiplier must be positive"
        }
        require(buyDieBeeD4BaseOddsValue >= 0.0) {
            "Buy D4 Bee-upgrade base odds cannot be negative"
        }
        require(buyDieBeeOddsGrowthPerTierValue > 0.0) {
            "Buy die Bee-upgrade odds growth must be positive"
        }
        require(buyPlantBeeBaseOddsValue >= 0.0) {
            "Buy Plant Bee-upgrade base odds cannot be negative"
        }
        require(buyWormBaseOddsAtTwoWormsValue >= 0.0) {
            "Buy Worm-upgrade base odds cannot be negative"
        }
        require(buyWormOddsMultiplierPerAdditionalWormValue > 0.0) {
            "Buy Worm odds multiplier must be positive"
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

        /**
         * Default willingness to spend Overgrowth on the best legal target.
         * These deliberately favor saving the Wisp for a larger die or a later
         * Battle opportunity instead of immediately consuming it on a D4/D6.
         */
        const val DEFAULT_OVERGROWTH_D4_USE_PERCENTAGE: Int = 5
        const val DEFAULT_OVERGROWTH_D6_USE_PERCENTAGE: Int = 10
        const val DEFAULT_OVERGROWTH_D8_USE_PERCENTAGE: Int = 20
        const val DEFAULT_OVERGROWTH_D10_USE_PERCENTAGE: Int = 40

        /**
         * Sunlight is a marginal alternative to drawing a D4. Once the visible
         * +3 can actually beat that expected draw, an ordinary player still
         * chooses the effect only about half the time.
         */
        const val DEFAULT_SUNLIGHT_USE_PERCENTAGE: Int = 50

        /**
         * Willingness to Mulch a die from Hand by its current showing value.
         * Five or higher is never voluntarily Mulched by the Human Baseline.
         */
        const val DEFAULT_MULCH_VALUE_1_USE_PERCENTAGE: Int = 80
        const val DEFAULT_MULCH_VALUE_2_USE_PERCENTAGE: Int = 60
        const val DEFAULT_MULCH_VALUE_3_USE_PERCENTAGE: Int = 40
        const val DEFAULT_MULCH_VALUE_4_USE_PERCENTAGE: Int = 20

        /**
         * Pocketed Spark is normally saved for Battle and/or a large discarded
         * die. A D6 in Cultivation is deliberately exceptional.
         */
        const val DEFAULT_POCKETED_SPARK_CULTIVATION_D4_USE_PERCENTAGE: Int = 0
        const val DEFAULT_POCKETED_SPARK_CULTIVATION_D6_USE_PERCENTAGE: Int = 5
        const val DEFAULT_POCKETED_SPARK_CULTIVATION_D8_USE_PERCENTAGE: Int = 10
        const val DEFAULT_POCKETED_SPARK_CULTIVATION_D10_USE_PERCENTAGE: Int = 20
        const val DEFAULT_POCKETED_SPARK_CULTIVATION_D12_USE_PERCENTAGE: Int = 50
        const val DEFAULT_POCKETED_SPARK_CULTIVATION_D20_USE_PERCENTAGE: Int = 80

        const val DEFAULT_POCKETED_SPARK_BATTLE_D4_USE_PERCENTAGE: Int = 5
        const val DEFAULT_POCKETED_SPARK_BATTLE_D6_USE_PERCENTAGE: Int = 15
        const val DEFAULT_POCKETED_SPARK_BATTLE_D8_USE_PERCENTAGE: Int = 30
        const val DEFAULT_POCKETED_SPARK_BATTLE_D10_USE_PERCENTAGE: Int = 50
        const val DEFAULT_POCKETED_SPARK_BATTLE_D12_USE_PERCENTAGE: Int = 75
        const val DEFAULT_POCKETED_SPARK_BATTLE_D20_USE_PERCENTAGE: Int = 95

        /**
         * Ordinary players strongly prefer to recover to a minimum two-card
         * Creature whenever they currently have fewer than two grafted Plants.
         * The 90% gate keeps rare "take the shiny die anyway" games possible.
         */
        const val DEFAULT_LOW_PLANT_PRIORITY_PERCENTAGE: Int = 90
        const val DEFAULT_LOW_PLANT_FLOOR: Int = 2

        /**
         * Buy-category balance treats each grafted Plant as roughly fifteen
         * die-side points. Thus the starting 30 die sides balance two Plants.
         */
        const val DEFAULT_BUY_PLANT_EQUIVALENT_DICE_POWER_PER_CARD: Int = 15

        /**
         * Logistic Buy-category tuning point. When one side is behind by this
         * many balance points, Human Baseline favors the weaker side 85/15.
         */
        const val DEFAULT_BUY_BALANCE_DIFFERENCE_FOR_85_PERCENT: Double = 3.0

        /**
         * Exponential preference between affordable Plant cost tiers. With the
         * normal six tiers (5, 7, 9, 11, 14, 17), 1.89 gives approximately
         * 2/4/7/13/25/48 percent from cheapest to most expensive.
         */
        const val DEFAULT_BUY_PLANT_COST_TIER_EXPONENTIAL_BASE: Double = 1.89

        /**
         * A deliberately cheaper-than-maximum Plant buy must leave at least this
         * much die value in Hand so a plausible follow-up purchase remains.
         */
        const val DEFAULT_BUY_CHEAPER_PLANT_MINIMUM_REMAINING_DICE: Int = 5

        /**
         * Buy-phase Bee use is intentionally softer than the normal two-Bee
         * reserve. Extra Bees multiply the odds of spending one to step a
         * purchase up by one tier. Doubling odds gives a useful intuitive
         * progression while never making one-Bee use mandatory.
         */
        const val DEFAULT_BUY_BEE_ODDS_MULTIPLIER_PER_ADDITIONAL_BEE: Double = 2.0

        /** One Bee gives a D4 -> D6 purchase about a 4% starting chance (odds 1:24). */
        const val DEFAULT_BUY_DIE_BEE_D4_BASE_ODDS: Double = 1.0 / 24.0

        /**
         * Each higher die tier multiplies the one-Bee odds by the cube root of 24.
         * This produces approximately 4%, 11%, 26%, 50% for D4/D6/D8/D10.
         */
        const val DEFAULT_BUY_DIE_BEE_ODDS_GROWTH_PER_TIER: Double = 2.8844991406148166

        /** One Bee gives an eligible Plant tier-step a 20% chance (odds 1:4). */
        const val DEFAULT_BUY_PLANT_BEE_BASE_ODDS: Double = 0.25

        /** Two Worms give a one-point Buy bridge about a 5% starting chance (odds 1:19). */
        const val DEFAULT_BUY_WORM_BASE_ODDS_AT_TWO_WORMS: Double = 1.0 / 19.0

        /** Each Worm beyond the second doubles the odds of spending one as a one-point bridge. */
        const val DEFAULT_BUY_WORM_ODDS_MULTIPLIER_PER_ADDITIONAL_WORM: Double = 2.0

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

    /**
     * Probability that an ordinary Human Baseline player is willing to spend
     * Overgrowth on the best currently legal target die. StrategyRandomizer
     * consumes the probability; this policy only supplies the tunable value.
     *
     * Keeping this overridable makes player-specific experimental behavior
     * straightforward later (for example, an enhanced player that saves D8s
     * more aggressively or spends D10s more readily in Battle).
     */
    open fun overgrowthUsePercentage(
        context: DecisionContext,
        targetSides: Int
    ): Int =
        when (targetSides) {
            4 -> overgrowthD4UsePercentageValue
            6 -> overgrowthD6UsePercentageValue
            8 -> overgrowthD8UsePercentageValue
            10 -> overgrowthD10UsePercentageValue
            else -> 0
        }

    /** Willingness to choose a worthwhile Sunlight Round Effect instead of drawing. */
    open fun sunlightUsePercentage(context: DecisionContext): Int =
        sunlightUsePercentageValue

    /** Willingness to Mulch a Hand die based on the value currently showing. */
    open fun mulchUsePercentage(
        context: DecisionContext,
        dieValue: Int
    ): Int =
        when (dieValue) {
            1 -> mulchValue1UsePercentageValue
            2 -> mulchValue2UsePercentageValue
            3 -> mulchValue3UsePercentageValue
            4 -> mulchValue4UsePercentageValue
            else -> 0
        }

    /**
     * Willingness to spend Pocketed Spark on the largest die currently in the
     * player's Discard. Cultivation is intentionally much more conservative
     * than Battle because the Wisp itself has later Battle timing value.
     */
    open fun pocketedSparkUsePercentage(
        context: DecisionContext,
        discardSides: Int
    ): Int {
        val battle = context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE
        return when (discardSides) {
            4 -> if (battle) pocketedSparkBattleD4UsePercentageValue else pocketedSparkCultivationD4UsePercentageValue
            6 -> if (battle) pocketedSparkBattleD6UsePercentageValue else pocketedSparkCultivationD6UsePercentageValue
            8 -> if (battle) pocketedSparkBattleD8UsePercentageValue else pocketedSparkCultivationD8UsePercentageValue
            10 -> if (battle) pocketedSparkBattleD10UsePercentageValue else pocketedSparkCultivationD10UsePercentageValue
            12 -> if (battle) pocketedSparkBattleD12UsePercentageValue else pocketedSparkCultivationD12UsePercentageValue
            20 -> if (battle) pocketedSparkBattleD20UsePercentageValue else pocketedSparkCultivationD20UsePercentageValue
            else -> 0
        }
    }

    /**
     * Strong state-based tendency to recover from having too few grafted Plants.
     *
     * This is deliberately independent of round number and Battle history. A player
     * who is knocked back below [lowPlantFloorValue] later in the game receives the
     * same protection as a player who has not yet built up their creature.
     *
     * Player-specific policies can override either this percentage method or the
     * configured floor/percentage constructor values to model a novice who is more
     * willing to remain dangerously Plant-light.
     */
    open fun lowPlantPriorityPercentage(context: DecisionContext): Int =
        if (context.self.board.plantCount < lowPlantFloorValue) {
            lowPlantPriorityPercentageValue
        } else {
            0
        }

    /** Visible Buy-balance value assigned to each grafted Plant. */
    open fun buyPlantEquivalentDicePowerPerCard(context: DecisionContext): Int =
        buyPlantEquivalentDicePowerPerCardValue

    /**
     * Relative weight for one affordable Plant cost tier. Rank zero is the
     * cheapest eligible tier, rank one the next-cheapest, and so on.
     *
     * Human Baseline intentionally uses price preference rather than a fixed
     * "always buy the most expensive Plant" rule. Player-specific policies may
     * override this to model different shopping personalities.
     */
    open fun buyPlantCostTierWeight(context: DecisionContext, rankFromCheapest: Int): Int {
        require(rankFromCheapest >= 0) { "Plant cost-tier rank cannot be negative" }
        return (1000.0 * buyPlantCostTierExponentialBaseValue.pow(rankFromCheapest))
            .roundToInt()
            .coerceAtLeast(1)
    }

    /** Minimum Hand die-value preserved by an intentionally cheaper Plant buy. */
    open fun buyCheaperPlantMinimumRemainingDice(context: DecisionContext): Int =
        buyCheaperPlantMinimumRemainingDiceValue

    /**
     * Chance to spend one Bee to step an otherwise-limited die purchase up by
     * one die tier. Only D4/D6/D8/D10 base purchases are eligible.
     *
     * With one Bee the defaults are approximately 4%, 11%, 26%, and 50%.
     * Each additional Bee doubles the odds, so three Bees make D10 -> D12 an
     * 80% decision while D4 -> D6 remains relatively uncommon (~14%).
     */
    open fun buyBeeUpgradeDiePercentage(
        context: DecisionContext,
        baseDie: dugsolutions.leaf.v35.random.die.DieSides,
        beeCount: Int
    ): Int {
        if (beeCount <= 0) return 0
        val tier = when (baseDie) {
            dugsolutions.leaf.v35.random.die.DieSides.D4 -> 0
            dugsolutions.leaf.v35.random.die.DieSides.D6 -> 1
            dugsolutions.leaf.v35.random.die.DieSides.D8 -> 2
            dugsolutions.leaf.v35.random.die.DieSides.D10 -> 3
            dugsolutions.leaf.v35.random.die.DieSides.D12,
            dugsolutions.leaf.v35.random.die.DieSides.D20 -> return 0
        }
        val odds = buyDieBeeD4BaseOddsValue *
            buyDieBeeOddsGrowthPerTierValue.pow(tier) *
            buyBeeOddsMultiplierPerAdditionalBeeValue.pow(beeCount - 1)
        return oddsToPercentage(odds)
    }

    /**
     * Chance to spend one Bee to bridge a Plant purchase to the next standard
     * Plant price tier when that tier is no more than two points above the
     * dice-group budget. The planner determines whether a useful tier is
     * actually reachable; this method supplies the willingness only.
     *
     * Defaults are 20% with one Bee, 33% with two, and 50% with three.
     */
    open fun buyBeeUpgradePlantPercentage(
        context: DecisionContext,
        beeCount: Int
    ): Int {
        if (beeCount <= 0) return 0
        val odds = buyPlantBeeBaseOddsValue *
            buyBeeOddsMultiplierPerAdditionalBeeValue.pow(beeCount - 1)
        return oddsToPercentage(odds)
    }

    /**
     * Chance to spend one Worm as a Buy bridge. Human Baseline only considers
     * this when it owns at least two Worms and the desired purchase is exactly
     * one point above the dice-group budget.
     *
     * Defaults are approximately 5%, 10%, 17%, 30% for 2/3/4/5 Worms.
     */
    open fun buyWormBridgePercentage(
        context: DecisionContext,
        wormCount: Int
    ): Int {
        if (wormCount < 2) return 0
        val odds = buyWormBaseOddsAtTwoWormsValue *
            buyWormOddsMultiplierPerAdditionalWormValue.pow(wormCount - 2)
        return oddsToPercentage(odds)
    }

    /**
     * Buy-plan penalty for wasting purchasing power through overpay. One point
     * is intentionally cheap; from two upward the concern grows exponentially.
     * Defaults: 0 -> 0, 1 -> 1, 2 -> 3, 3 -> 7, 4 -> 15.
     */
    open fun buyOverpayPenalty(context: DecisionContext, overpay: Int): Int {
        require(overpay >= 0) { "Buy overpay cannot be negative" }
        if (overpay == 0) return 0
        return (2.0.pow(overpay).roundToInt() - 1).coerceAtMost(1_000_000)
    }

    private fun oddsToPercentage(odds: Double): Int {
        if (odds <= 0.0) return 0
        val probability = odds / (1.0 + odds)
        return (probability * 100.0).roundToInt().coerceIn(0, 99)
    }

    /**
     * Probability of preferring the Plant category when both a Plant and a die
     * are affordable during Buy.
     *
     * This deliberately ignores round number. It compares the player's visible
     * long-term assets directly:
     *
     * `Plant power = grafted Plant count * 15`
     * `Dice power = total sides of all owned dice`
     *
     * Positive difference means dice are ahead and therefore Plants deserve the
     * stronger pull. The logistic curve is symmetric: equal development is
     * 50/50; a three-point difference is 85/15 toward the weaker side.
     * The default result is capped at 1..99 to preserve a small amount of
     * ordinary-player variation under even a severe imbalance.
     */
    open fun buyPlantPriorityPercentage(context: DecisionContext): Int {
        val plantPower =
            context.self.board.plantCount * buyPlantEquivalentDicePowerPerCard(context)
        val dicePower = context.self.board.dicePower
        val difference = dicePower - plantPower
        if (difference == 0) return 50

        val slope = ln(85.0 / 15.0) / buyBalanceDifferenceFor85PercentValue
        val probability = 100.0 / (1.0 + exp(-slope * difference))
        // Preserve a tiny amount of ordinary-player variation even under a
        // severe imbalance. Player-specific policies may still override this
        // method and return a literal 0 or 100 when desired.
        return probability.roundToInt().coerceIn(1, 99)
    }

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

