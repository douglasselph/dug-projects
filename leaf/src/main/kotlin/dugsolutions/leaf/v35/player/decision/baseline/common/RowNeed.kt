package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.max

/**
 * Human-readable snapshot of how much attention one Battle row needs now.
 *
 * [currentlyWinning] follows the actual Strike rule: a tied high score counts
 * as winning when at least one lower participant exists; if everyone ties,
 * nobody is currently winning.
 */
data class RowNeed(
    val row: StrikeRow,
    val available: Boolean,
    val ownTotal: Int,
    val leadingOpponentTotal: Int?,
    /** ownTotal - highest participating opponent total. */
    val margin: Int,
    val needScore: Int,
    val currentlyWinning: Boolean,
    val woundRisk: Boolean,
    val pointsToBecomeWinner: Int,
    val pointsToAvoidWound: Int
)

/** Initial tunable weights; callers can replace these without changing rules. */
data class RowNeedWeights(
    val secureWinner: Int = 15,
    val narrowWinner: Int = 30,
    val tiedWinner: Int = 45,
    val closeLossBase: Int = 100,
    val lossPenaltyPerExtraPointNeeded: Int = 8,
    val minimumLosingNeed: Int = 20,
    val woundRiskBonus: Int = 15
) {
    init {
        require(
            listOf(
                secureWinner,
                narrowWinner,
                tiedWinner,
                closeLossBase,
                lossPenaltyPerExtraPointNeeded,
                minimumLosingNeed,
                woundRiskBonus
            ).all { it >= 0 }
        ) { "Row Need weights cannot be negative" }
    }
}

object RowNeedHeuristics {
    private const val WOUND_MARGIN = 5

    fun calculate(
        context: DecisionContext,
        row: StrikeRow,
        weights: RowNeedWeights = RowNeedWeights()
    ): RowNeed {
        val battle = context.battle
            ?: return unavailable(row)
        return calculate(
            battle = battle,
            actorId = context.self.id,
            row = row,
            weights = weights
        )
    }

    fun calculateAll(
        context: DecisionContext,
        weights: RowNeedWeights = RowNeedWeights()
    ): Map<StrikeRow, RowNeed> =
        StrikeRow.entries.associateWith { row ->
            calculate(context, row, weights)
        }

    fun calculate(
        battle: BattleView,
        actorId: PlayerId,
        row: StrikeRow,
        weights: RowNeedWeights = RowNeedWeights()
    ): RowNeed = calculate(
        rowView = battle.row(row),
        actorId = actorId,
        weights = weights
    )

    fun calculate(
        rowView: BattleRowView,
        actorId: PlayerId,
        weights: RowNeedWeights = RowNeedWeights()
    ): RowNeed {
        val own = rowView.forPlayer(actorId)
            ?: return unavailable(rowView.row)

        if (rowView.closed || own.withdrawn) {
            return unavailable(rowView.row, own.total)
        }

        val participating = rowView.players.filterNot { it.withdrawn }
        val opponents = participating.filterNot { it.playerId == actorId }
        val ownTotal = own.total
        val leadingOpponent = opponents.maxOfOrNull { it.total }
        val margin = ownTotal - (leadingOpponent ?: 0)

        val high = participating.maxOfOrNull { it.total } ?: ownTotal
        val highPlayers = participating.filter { it.total == high }
        val everyoneTied = participating.size > 1 && highPlayers.size == participating.size
        val currentlyWinning = !everyoneTied && highPlayers.any { it.playerId == actorId }

        val woundRisk = !currentlyWinning && high - ownTotal >= WOUND_MARGIN
        val pointsToAvoidWound = if (woundRisk) {
            max(0, (high - (WOUND_MARGIN - 1)) - ownTotal)
        } else {
            0
        }

        val pointsToBecomeWinner = when {
            currentlyWinning -> 0
            // Any +1 breaks an all-player tie and becomes the sole high score.
            everyoneTied -> 1
            ownTotal < high -> (high + 1) - ownTotal
            else -> 0
        }

        val needScore = when {
            opponents.isEmpty() -> 0
            currentlyWinning && margin >= WOUND_MARGIN -> weights.secureWinner
            currentlyWinning && margin > 0 -> weights.narrowWinner
            currentlyWinning -> weights.tiedWinner
            else -> {
                val extraNeeded = max(0, pointsToBecomeWinner - 1)
                val base = max(
                    weights.minimumLosingNeed,
                    weights.closeLossBase -
                        extraNeeded * weights.lossPenaltyPerExtraPointNeeded
                )
                base + if (woundRisk) weights.woundRiskBonus else 0
            }
        }

        return RowNeed(
            row = rowView.row,
            available = true,
            ownTotal = ownTotal,
            leadingOpponentTotal = leadingOpponent,
            margin = margin,
            needScore = needScore,
            currentlyWinning = currentlyWinning,
            woundRisk = woundRisk,
            pointsToBecomeWinner = pointsToBecomeWinner,
            pointsToAvoidWound = pointsToAvoidWound
        )
    }

    private fun unavailable(
        row: StrikeRow,
        ownTotal: Int = 0
    ): RowNeed =
        RowNeed(
            row = row,
            available = false,
            ownTotal = ownTotal,
            leadingOpponentTotal = null,
            margin = 0,
            needScore = 0,
            currentlyWinning = false,
            woundRisk = false,
            pointsToBecomeWinner = 0,
            pointsToAvoidWound = 0
        )
}
