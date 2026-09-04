package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.WoundResolution
import dugsolutions.leaf.v35.game.operation.WoundResolver
import dugsolutions.leaf.v35.player.PlayerId

/** Immutable per-player score snapshot for one Strike Row. */
data class StrikePlayerTotal(
    val playerId: PlayerId,
    val diceTotal: Int,
    val critterTotal: Int
) {
    val total: Int
        get() = diceTotal + critterTotal
}

data class StrikeWoundResult(
    val playerId: PlayerId,
    val resolution: WoundResolution
)

/** Result of resolving one open Strike Row. */
data class StrikeResolution(
    val row: StrikeRow,
    val totals: List<StrikePlayerTotal>,
    val winnerIds: List<PlayerId>,
    val wounds: List<StrikeWoundResult>,
    val vpPerWinner: Int
) {
    val woundedPlayerIds: List<PlayerId>
        get() = wounds.map { it.playerId }

    val winningTotal: Int?
        get() = winnerIds.firstOrNull()?.let { winnerId ->
            totals.first { it.playerId == winnerId }.total
        }
}

data class BattleStrikeResolutionResult(
    val strikes: List<StrikeResolution>
) {
    val totalWounds: Int
        get() = strikes.sumOf { it.wounds.size }
}

/**
 * Resolves Battle Step 6 without changing Grid placements.
 *
 * Every player still participating in an open row contributes that square's
 * total. Root & Scoot withdrawals are player-specific and are omitted entirely
 * from that Strike; a globally closed row is still skipped by [resolveAll].
 */
class StrikeResolver(
    private val woundResolver: WoundResolver
) {
    companion object {
        private const val BASE_STRIKE_VP = 2
        private const val WOUND_MARGIN = 5
    }

    /** Resolve every currently open Strike Row from TOP to BOTTOM. */
    fun resolveAll(
        game: Game,
        battleState: BattleState
    ): BattleStrikeResolutionResult =
        BattleStrikeResolutionResult(
            strikes = StrikeRow.entries
                .filterNot { battleState.grid.isRowClosed(it) }
                .map { row ->
                    resolveRow(
                        game = game,
                        battleState = battleState,
                        row = row
                    )
                }
        )

    /**
     * Resolve exactly one open Strike Row.
     *
     * This public seam is intentional: Wisp's Last Word will resolve one row
     * immediately and then close it.
     */
    fun resolveRow(
        game: Game,
        battleState: BattleState,
        row: StrikeRow
    ): StrikeResolution {
        stateCheck(
            !battleState.grid.isRowClosed(row),
            context = "StrikeResolver"
        ) {
            "Cannot resolve closed Strike Row $row"
        }

        val totals = battleState.playersInBattleOrder
            .filterNot { player ->
                battleState.grid.isPlayerWithdrawn(player.id, row)
            }
            .map { player ->
                val square = battleState.grid.square(player.id, row)
                StrikePlayerTotal(
                    playerId = player.id,
                    diceTotal = square.dice.sumOf { it.value },
                    critterTotal = square.critters.sumOf {
                        player.critterValues.valueOf(it)
                    }
                )
            }

        val high = totals.maxOfOrNull { it.total }
        val highPlayers =
            if (high == null) {
                emptyList()
            } else {
                totals.filter { it.total == high }
            }
        val everyoneTied =
            totals.size > 1 && highPlayers.size == totals.size

        val winners =
            if (everyoneTied) {
                emptyList()
            } else {
                highPlayers
            }

        val wounds =
            if (winners.isEmpty()) {
                emptyList()
            } else {
                val winningTotal = winners.first().total
                totals
                    .filter { total ->
                        total.playerId !in winners.map { it.playerId } &&
                            winningTotal - total.total >= WOUND_MARGIN
                    }
                    .map { wounded ->
                        val player = battleState.player(wounded.playerId)
                        StrikeWoundResult(
                            playerId = wounded.playerId,
                            resolution = woundResolver.resolve(player)
                        )
                    }
            }

        val vpPerWinner =
            if (winners.isEmpty()) {
                0
            } else {
                BASE_STRIKE_VP + wounds.size
            }

        winners.forEach { winner ->
            battleState.player(winner.playerId).addVp(vpPerWinner)
        }

        val winnerIds = winners.map { it.playerId }
        val woundedIds = wounds.map { it.playerId }

        game.chronicle.record(
            Moment.Marker(
                "STRIKE row=$row totals=" +
                    totals.joinToString(",") { "${it.playerId.value}:${it.total}" } +
                    " winners=" +
                    (if (winnerIds.isEmpty()) {
                        "NONE"
                    } else {
                        winnerIds.joinToString(",") { it.value.toString() }
                    }) +
                    " wounded=" +
                    (if (woundedIds.isEmpty()) {
                        "NONE"
                    } else {
                        woundedIds.joinToString(",") { it.value.toString() }
                    }) +
                    " vpEach=$vpPerWinner"
            )
        )

        return StrikeResolution(
            row = row,
            totals = totals,
            winnerIds = winnerIds,
            wounds = wounds,
            vpPerWinner = vpPerWinner
        )
    }
}
