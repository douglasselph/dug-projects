package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.operation.RollRewardPolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Consequences of ONE current-round action, before any future-round forecast.
 *
 * [spendingPips] are real expected face-value pips available in the current
 * Hand. Critter/Wisp values are provisional *strategic equivalents*, not
 * additional pips that can be spent on a purchase or placed in a Strike.
 * Signed spending changes are meaningful for rerolls and replacements.
 */
data class ImmediatePipValue(
    val spendingPips: Double,
    val critterOptionValue: Double = 0.0,
    val wispOptionValue: Double = 0.0
) {
    init {
        require(spendingPips.isFinite() && critterOptionValue.isFinite() && wispOptionValue.isFinite()) {
            "Immediate pip values must be finite"
        }
        require(critterOptionValue >= 0 && wispOptionValue >= 0) {
            "Roll-reward option values cannot be negative"
        }
    }

    val pipEquivalentTotal: Double get() = spendingPips + critterOptionValue + wispOptionValue

    operator fun plus(other: ImmediatePipValue): ImmediatePipValue = ImmediatePipValue(
        spendingPips + other.spendingPips,
        critterOptionValue + other.critterOptionValue,
        wispOptionValue + other.wispOptionValue
    )
}

/**
 * Visible availability for an ordinary roll. These are strategic *estimates*
 * of reward access, not substitutes for the engine's physical award logic.
 *
 * The provisional +2 Critter and +3 Wisp equivalents are hypotheses. Critter
 * choice, immediate Wisp effects, supply depletion, and the five-Wisp reserve
 * limit can make actual usefulness different. Future choices remain unmodeled.
 */
data class RollRewardOpportunities(
    val critterAvailable: Boolean,
    val wispAvailable: Boolean,
    val critterPipEquivalent: Double = 2.0,
    val wispPipEquivalent: Double = 3.0
) {
    companion object {
        private const val STRATEGIC_WISP_CAP = 5

        /** The engine's NORMAL roll rewards apply in both Cultivation and Battle. */
        fun fromVisibleContext(context: DecisionContext): RollRewardOpportunities =
            RollRewardOpportunities(
                critterAvailable = context.grove.bees > 0 || context.grove.worms > 0,
                wispAvailable = context.grove.wispDeckRemaining > 0 &&
                    context.self.wispCount < STRATEGIC_WISP_CAP
            )
    }

    init {
        require(critterPipEquivalent.isFinite() && critterPipEquivalent >= 0) {
            "Critter equivalent must be finite and nonnegative"
        }
        require(wispPipEquivalent.isFinite() && wispPipEquivalent >= 0) {
            "Wisp equivalent must be finite and nonnegative"
        }
    }
}

/**
 * Shared deterministic arithmetic for immediate Draw, roll, reroll, replacement,
 * and face-changing effects. Does not consume RNG, alter game state, assign
 * Build priority, or anticipate ANY future rounds.
 *
 * The caller supplies legal candidates and the *actual* RollRewardPolicy:
 * IGNORE earns no rewards; DEFER is conservatively zero until a kept roll is
 * committed. Merely changing a face (Raise/flip/set-to-max) never awards a new
 * Roll Reward. Availability must be drawn from the relevant visible game state.
 */
object ImmediatePipValuation {
    private val physicalSides = setOf(4, 6, 8, 10, 12, 20)

    fun roll(
        sides: Int,
        opportunities: RollRewardOpportunities,
        rewardPolicy: RollRewardPolicy = RollRewardPolicy.NORMAL
    ): ImmediatePipValue {
        requirePhysicalSides(sides)
        return rollValue(
            CultivationPipOutcome.expectedNewDie(sides), sides, opportunities, rewardPolicy
        )
    }

    /** A Draw adds a new die to Hand, rather than replacing an existing result. */
    fun draws(
        supply: List<Int>,
        discard: List<Int>,
        count: Int,
        opportunities: RollRewardOpportunities,
        rewardPolicy: RollRewardPolicy = RollRewardPolicy.NORMAL
    ): ImmediatePipValue = ExpectedDrawProjection.drawSides(supply, discard, count)
        .fold(ImmediatePipValue(0.0)) { total, sides ->
            total + roll(sides, opportunities, rewardPolicy)
        }

    /** Reroll retains the die: count ONLY the change from its current result. */
    fun reroll(
        sides: Int,
        currentValue: Int,
        opportunities: RollRewardOpportunities,
        rewardPolicy: RollRewardPolicy = RollRewardPolicy.NORMAL
    ): ImmediatePipValue {
        requirePhysicalSides(sides)
        val spendingGain = DieValueHeuristics.expectedRerollGain(sides, currentValue)
        return rollValue(spendingGain, sides, opportunities, rewardPolicy)
    }

    /** An Upgrade-and-use-now replaces an existing face with a fresh roll. */
    fun upgradeUseNow(
        oldSides: Int,
        currentValue: Int,
        newSides: Int,
        opportunities: RollRewardOpportunities,
        rewardPolicy: RollRewardPolicy = RollRewardPolicy.NORMAL
    ): ImmediatePipValue {
        requirePhysicalSides(oldSides)
        requirePhysicalSides(newSides)
        require(newSides > oldSides) { "Upgrade replacement must be larger" }
        val spendingGain = ExpectedDrawProjection.upgradeDelta(oldSides, currentValue, newSides)
        return rollValue(spendingGain, newSides, opportunities, rewardPolicy)
    }

    /** Deterministic Raise: capped spending gain, no new roll and no reward. */
    fun raise(sides: Int, currentValue: Int, amount: Int): ImmediatePipValue {
        requirePhysicalSides(sides)
        return ImmediatePipValue(CultivationPipOutcome.raisedExistingDie(sides, currentValue, amount).toDouble())
    }

    /** Existing supported face effects. Null means NOT MODELED, never zero. */
    fun faceEffect(effect: GameEffect, sides: Int, currentValue: Int): ImmediatePipValue? {
        requirePhysicalSides(sides)
        return SharedDieEffectOutcome.projectedPips(effect, sides, currentValue)
            ?.let { ImmediatePipValue(it.toDouble()) }
    }

    private fun requirePhysicalSides(sides: Int) {
        require(sides in physicalSides) { "Unsupported physical die: D$sides" }
    }

    private fun rollValue(
        spendingPips: Double,
        sides: Int,
        opportunities: RollRewardOpportunities,
        rewardPolicy: RollRewardPolicy
    ): ImmediatePipValue {
        val normalReward = rewardPolicy == RollRewardPolicy.NORMAL
        return ImmediatePipValue(
            spendingPips = spendingPips,
            critterOptionValue = if (normalReward && opportunities.critterAvailable) {
                opportunities.critterPipEquivalent / sides
            } else 0.0,
            wispOptionValue = if (normalReward && opportunities.wispAvailable) {
                opportunities.wispPipEquivalent / sides
            } else 0.0
        )
    }
}
