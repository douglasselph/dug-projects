package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/**
 * Stable opportunity-cost reference for drawing the next die.
 *
 * The scored utility deliberately remains simple: Draw is worth a fixed action
 * baseline plus the expected value of the next publicly knowable die.  The
 * accompanying [DrawReferenceObservation] audits other visible state that may
 * matter to later calibration without silently turning those observations into
 * speculative priority bonuses.
 */
object DrawPriority {
    private const val NO_DIE_SCORE = 20
    private const val BASE_SCORE = 35
    private const val POINTS_PER_EXPECTED_VALUE = 4

    fun score(context: DecisionContext): PriorityScore {
        val observation = observe(context)
        val nextSides = observation.nextDieSides ?: return PriorityScore(NO_DIE_SCORE)
            .adjusted(0, "No die is available to draw")
        val expectedContribution = (POINTS_PER_EXPECTED_VALUE * observation.expectedRoll!!).roundToInt()
        return PriorityScore(BASE_SCORE)
            .adjusted(expectedContribution, "Expected D$nextSides roll value")
            .adjusted(0, "Draw reference: ${observation.summary()}")
    }

    /** Typed, side-effect-free audit of player-visible state relevant to Draw. */
    fun observe(context: DecisionContext): DrawReferenceObservation {
        val board = context.self.board
        val nextSides = board.supply.minOfOrNull { it.sides }
            ?: board.discard.minOfOrNull { it.sides }
        val owned = board.supply + board.hand + board.discard
        val currentPower = PurchaseThresholdHeuristics.purchasingPower(board)
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
        return DrawReferenceObservation(
            nextDieSides = nextSides,
            expectedRoll = nextSides?.let(DieValueHeuristics::expectedRoll),
            rollRewardChance = nextSides?.let { minOf(2, it).toDouble() / it.toDouble() },
            ownedDiceCount = owned.size + board.mulch.count { it.storedDieSides != null } +
                board.pendingMulch.count { it.storedDieSides != null },
            averageOwnedDieSides = owned.map { it.sides }.averageOrNull(),
            cultivationRoundsRemaining = context.progress.cultivationRoundsRemaining,
            currentPurchasingPower = currentPower,
            currentAffordableTier = PurchaseThresholdHeuristics.bestAffordableTier(currentPower, tiers),
            availableBuyTiers = tiers
        )
    }

    private fun List<Int>.averageOrNull(): Double? =
        if (isEmpty()) null else average()
}

/**
 * Player-visible Draw context captured for diagnostics/calibration.
 *
 * Fields other than next-die expected value are observations, not score terms.
 * Battle proximity is intentionally absent until shared next-phase awareness is
 * introduced from information an ordinary player is entitled to know.
 */
data class DrawReferenceObservation(
    val nextDieSides: Int?,
    val expectedRoll: Double?,
    val rollRewardChance: Double?,
    val ownedDiceCount: Int,
    val averageOwnedDieSides: Double?,
    val cultivationRoundsRemaining: Int?,
    val currentPurchasingPower: Int,
    val currentAffordableTier: Int?,
    val availableBuyTiers: List<Int>
) {
    fun summary(): String = buildString {
        append("ownedDice=").append(ownedDiceCount)
        averageOwnedDieSides?.let { append(", avgSides=").append("%.2f".format(it)) }
        rollRewardChance?.let { append(", rollRewardChance=").append("%.3f".format(it)) }
        cultivationRoundsRemaining?.let { append(", cultivationRoundsRemaining=").append(it) }
        append(", buyPower=").append(currentPurchasingPower)
        append(", affordableTier=").append(currentAffordableTier ?: "none")
    }
}
