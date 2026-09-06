package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.player.decision.context.GroveView
import dugsolutions.leaf.v35.player.decision.context.PlayerBoardView
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Describes how a value change moves purchasing power through currently
 * relevant single-item cost tiers.
 */
data class PurchaseThresholdChange(
    val beforePower: Int,
    val afterPower: Int,
    val beforeTier: Int?,
    val afterTier: Int?,
    val crossedUp: List<Int>,
    val crossedDown: List<Int>
) {
    val tierSteps: Int
        get() = crossedUp.size - crossedDown.size

    val improved: Boolean get() = tierSteps > 0
    val worsened: Boolean get() = tierSteps < 0
}

/** Shared helpers for asking whether a die/action crosses a Buy threshold. */
object PurchaseThresholdHeuristics {

    /** Current Buy power from Hand dice plus owned Critters at current values. */
    fun purchasingPower(board: PlayerBoardView): Int =
        board.hand.sumOf { it.value } +
            board.bees * board.beeValue +
            board.worms * board.wormValue

    /**
     * Cost tiers physically available in the Grove right now. D4 is excluded
     * because its Graft Bed space is a return space rather than a normal Buy.
     */
    fun availableCostTiers(grove: GroveView): List<Int> =
        tiers(
            grove.plantStacks
                .filter { it.remaining > 0 }
                .map { it.cost } +
                grove.graftBed
                    .filter { (sides, count) -> sides != DieSides.D4 && count > 0 }
                    .keys
                    .map { it.value }
        )

    /** Sorted unique positive cost tiers. */
    fun tiers(costs: Iterable<Int>): List<Int> =
        costs
            .filter { it > 0 }
            .distinct()
            .sorted()

    fun bestAffordableTierBefore(
        purchasingPower: Int,
        costs: Iterable<Int>
    ): Int? = bestAffordableTier(purchasingPower, costs)

    fun bestAffordableTierAfter(
        purchasingPower: Int,
        valueChange: Int,
        costs: Iterable<Int>
    ): Int? = bestAffordableTier(
        purchasingPower = purchasingPower + valueChange,
        costs = costs
    )

    fun bestAffordableTier(
        purchasingPower: Int,
        costs: Iterable<Int>
    ): Int? {
        require(purchasingPower >= 0) {
            "Purchasing power cannot be negative: $purchasingPower"
        }
        return tiers(costs).lastOrNull { it <= purchasingPower }
    }

    /** Full threshold movement, including every newly reached/lost tier. */
    fun change(
        beforePower: Int,
        afterPower: Int,
        costs: Iterable<Int>
    ): PurchaseThresholdChange {
        require(beforePower >= 0) { "Before purchasing power cannot be negative" }
        require(afterPower >= 0) { "After purchasing power cannot be negative" }

        val normalized = tiers(costs)
        val crossedUp = if (afterPower > beforePower) {
            normalized.filter { it > beforePower && it <= afterPower }
        } else {
            emptyList()
        }
        val crossedDown = if (afterPower < beforePower) {
            normalized.filter { it > afterPower && it <= beforePower }.asReversed()
        } else {
            emptyList()
        }

        return PurchaseThresholdChange(
            beforePower = beforePower,
            afterPower = afterPower,
            beforeTier = normalized.lastOrNull { it <= beforePower },
            afterTier = normalized.lastOrNull { it <= afterPower },
            crossedUp = crossedUp,
            crossedDown = crossedDown
        )
    }

    /**
     * Small tunable score primitive: each cost tier gained is positive and each
     * tier lost is negative. Higher-level scorers decide the points-per-tier.
     */
    fun thresholdBonus(
        beforePower: Int,
        afterPower: Int,
        costs: Iterable<Int>,
        pointsPerTier: Int = 10
    ): Int {
        require(pointsPerTier >= 0) {
            "Threshold points per tier cannot be negative: $pointsPerTier"
        }
        return change(beforePower, afterPower, costs).tierSteps * pointsPerTier
    }
}
