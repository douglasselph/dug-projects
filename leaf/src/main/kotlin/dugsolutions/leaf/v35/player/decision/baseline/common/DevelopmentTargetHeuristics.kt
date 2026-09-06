package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Initial soft Human Baseline development curve.
 *
 * These are tuning assumptions, not game rules and not measurements of card
 * strength. They are deliberately centralized so simulation can revise them
 * without changing individual action/card scorers.
 */
data class DevelopmentTargetConfig(
    /** Default starting pool is 3D4 + 3D6 = 30 total die sides. */
    val startingDicePower: Int = 30,
    /** Neutral target: roughly six Plants by the end of an 8-round Cultivation. */
    val plantsPerCultivationRound: Double = 0.75,
    /** Neutral target: add roughly three die-side points of pool power per round. */
    val dicePowerGainPerCultivationRound: Double = 3.0
) {
    init {
        require(startingDicePower >= 0) { "Starting dice power cannot be negative" }
        require(plantsPerCultivationRound >= 0.0) {
            "Plants-per-round target cannot be negative"
        }
        require(dicePowerGainPerCultivationRound >= 0.0) {
            "Dice-power gain target cannot be negative"
        }
    }
}

data class DevelopmentTarget(
    /** Cultivation round whose end-state the current decision is aiming toward. */
    val cultivationMilestone: Int,
    val targetPlantCount: Int,
    val targetDicePower: Int,
    val currentPlantCount: Int,
    val currentDicePower: Int
) {
    val plantDeficit: Int get() = max(0, targetPlantCount - currentPlantCount)
    val dicePowerDeficit: Int get() = max(0, targetDicePower - currentDicePower)
    val plantSurplus: Int get() = max(0, currentPlantCount - targetPlantCount)
    val dicePowerSurplus: Int get() = max(0, currentDicePower - targetDicePower)
}

object DevelopmentTargetHeuristics {

    fun cultivationMilestone(
        progress: GameProgressView,
        phase: RoundCardType?
    ): Int =
        when {
            phase == RoundCardType.CULTIVATION &&
                progress.currentCultivationRoundNumber != null ->
                progress.currentCultivationRoundNumber

            phase == RoundCardType.BATTLE &&
                progress.totalCultivationRounds != null ->
                progress.totalCultivationRounds

            else -> progress.cultivationRoundsCompleted
        }.coerceAtLeast(0)

    fun targetPlantCount(
        milestone: Int,
        config: DevelopmentTargetConfig = DevelopmentTargetConfig()
    ): Int {
        require(milestone >= 0) { "Cultivation milestone cannot be negative" }
        return ceil(milestone * config.plantsPerCultivationRound).toInt()
    }

    fun targetDicePower(
        milestone: Int,
        config: DevelopmentTargetConfig = DevelopmentTargetConfig()
    ): Int {
        require(milestone >= 0) { "Cultivation milestone cannot be negative" }
        return config.startingDicePower +
            (milestone * config.dicePowerGainPerCultivationRound).roundToInt()
    }

    fun assess(
        context: DecisionContext,
        config: DevelopmentTargetConfig = DevelopmentTargetConfig()
    ): DevelopmentTarget {
        val milestone = cultivationMilestone(context.progress, context.phase)
        return DevelopmentTarget(
            cultivationMilestone = milestone,
            targetPlantCount = targetPlantCount(milestone, config),
            targetDicePower = targetDicePower(milestone, config),
            currentPlantCount = context.self.board.plantCount,
            currentDicePower = context.self.board.dicePower
        )
    }
}
