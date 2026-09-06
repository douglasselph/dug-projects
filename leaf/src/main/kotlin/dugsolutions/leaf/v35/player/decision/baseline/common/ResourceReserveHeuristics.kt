package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.PlayerBoardView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import kotlin.math.max

/** Spendable resource types for which Human Baseline may keep a soft reserve. */
enum class ReserveResource {
    BEE,
    WORM,
    WATER,
    MULCH
}

data class ResourceReserveTargets(
    val bees: Int = 0,
    val worms: Int = 0,
    val water: Int = 0,
    val mulch: Int = 0
) {
    init {
        require(bees >= 0 && worms >= 0 && water >= 0 && mulch >= 0) {
            "Resource reserve targets cannot be negative"
        }
    }

    fun forResource(resource: ReserveResource): Int =
        when (resource) {
            ReserveResource.BEE -> bees
            ReserveResource.WORM -> worms
            ReserveResource.WATER -> water
            ReserveResource.MULCH -> mulch
        }
}

/**
 * Tunable neutral reserve policy. The final Battle deliberately reserves
 * nothing because these resources have no post-game conversion by default.
 */
data class ResourceReservePolicy(
    val cultivation: ResourceReserveTargets = ResourceReserveTargets(
        bees = 1,
        worms = 1,
        water = 1,
        mulch = 1
    ),
    val battle: ResourceReserveTargets = ResourceReserveTargets(
        bees = 1,
        worms = 1,
        water = 1,
        mulch = 1
    ),
    val finalBattle: ResourceReserveTargets = ResourceReserveTargets()
)

data class ResourceReserveStatus(
    val resource: ReserveResource,
    val current: Int,
    val target: Int
) {
    init {
        require(current >= 0) { "Current resource count cannot be negative" }
        require(target >= 0) { "Resource reserve target cannot be negative" }
    }

    val deficit: Int get() = max(0, target - current)
    val surplus: Int get() = max(0, current - target)
    val atOrBelowReserve: Boolean get() = current <= target

    fun remainingAfterSpend(amount: Int = 1): Int {
        require(amount >= 0) { "Spend amount cannot be negative: $amount" }
        return max(0, current - amount)
    }

    fun unitsBelowReserveAfterSpend(amount: Int = 1): Int =
        max(0, target - remainingAfterSpend(amount))
}

object ResourceReserveHeuristics {

    fun targets(
        context: DecisionContext,
        policy: ResourceReservePolicy = ResourceReservePolicy()
    ): ResourceReserveTargets =
        when {
            context.phase == RoundCardType.BATTLE &&
                context.progress.isFinalBattleRound -> policy.finalBattle

            context.phase == RoundCardType.BATTLE -> policy.battle
            else -> policy.cultivation
        }

    fun currentCount(
        board: PlayerBoardView,
        resource: ReserveResource
    ): Int =
        when (resource) {
            ReserveResource.BEE -> board.bees
            ReserveResource.WORM -> board.worms
            ReserveResource.WATER -> board.water
            ReserveResource.MULCH -> board.mulch.size + board.pendingMulch.size
        }

    fun status(
        board: PlayerBoardView,
        resource: ReserveResource,
        targets: ResourceReserveTargets
    ): ResourceReserveStatus =
        ResourceReserveStatus(
            resource = resource,
            current = currentCount(board, resource),
            target = targets.forResource(resource)
        )

    fun status(
        context: DecisionContext,
        resource: ReserveResource,
        policy: ResourceReservePolicy = ResourceReservePolicy()
    ): ResourceReserveStatus =
        status(
            board = context.self.board,
            resource = resource,
            targets = targets(context, policy)
        )

    fun statuses(
        context: DecisionContext,
        policy: ResourceReservePolicy = ResourceReservePolicy()
    ): Map<ReserveResource, ResourceReserveStatus> {
        val desired = targets(context, policy)
        return ReserveResource.entries.associateWith { resource ->
            status(context.self.board, resource, desired)
        }
    }

    /** Positive tuning primitive for filling an existing reserve deficit. */
    fun acquireBonus(
        status: ResourceReserveStatus,
        amount: Int = 1,
        pointsPerFilledUnit: Int = 10
    ): Int {
        require(amount >= 0) { "Acquire amount cannot be negative: $amount" }
        require(pointsPerFilledUnit >= 0) {
            "Acquire bonus per unit cannot be negative: $pointsPerFilledUnit"
        }
        val filled = minOf(status.deficit, amount)
        return filled * pointsPerFilledUnit
    }

    /** Negative tuning primitive for spending into/below the desired reserve. */
    fun spendPenalty(
        status: ResourceReserveStatus,
        amount: Int = 1,
        pointsPerMissingUnit: Int = 10
    ): Int {
        require(amount >= 0) { "Spend amount cannot be negative: $amount" }
        require(pointsPerMissingUnit >= 0) {
            "Spend penalty per unit cannot be negative: $pointsPerMissingUnit"
        }
        return -status.unitsBelowReserveAfterSpend(amount) * pointsPerMissingUnit
    }
}
