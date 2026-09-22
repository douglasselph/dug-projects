package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.BattleSquare
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter

/** Normalized remaining Support moves, counted by physical resource instance. */
data class BattleSupportCapacity(
    val wispMoves: Int = 0,
    val waterMoves: Int = 0,
    val mulchMoves: Int = 0,
    val wormMoves: Int = 0,
    val beeMoves: Int = 0,
    val butterflyMoves: Int = 0
) {
    init {
        require(
            listOf(
                wispMoves,
                waterMoves,
                mulchMoves,
                wormMoves,
                beeMoves,
                butterflyMoves
            ).all { it >= 0 }
        ) {
            "Battle Support move counts cannot be negative: $this"
        }
    }

    val totalMoves: Int
        get() =
            wispMoves + waterMoves + mulchMoves +
                wormMoves + beeMoves + butterflyMoves

    companion object {
        val ZERO = BattleSupportCapacity()
    }
}

/**
 * Own exact legal capacity plus public opponent capacity for the current Battle state.
 *
 * [relevantLiveThreatOpponents] is deliberately supplied by the caller. B9 counts the
 * capacity of that set; the later continuation/tempo checkpoint owns deciding which
 * close rows and Live Threats are relevant.
 */
data class BattleSupportCapacityAssessment(
    val own: BattleSupportCapacity,
    val opponents: Map<PlayerId, BattleSupportCapacity>,
    val relevantLiveThreatOpponents: Map<PlayerId, BattleSupportCapacity>
) {
    val strongestRelevantOpponentCapacity: Int =
        relevantLiveThreatOpponents.values.maxOfOrNull { it.totalMoves } ?: 0

    val strongestRelevantOpponentIds: Set<PlayerId> =
        relevantLiveThreatOpponents
            .filterValues { it.totalMoves == strongestRelevantOpponentCapacity }
            .keys
            .toSet()
}

/**
 * Counts remaining Battle Support moves without counting target multiplicity.
 *
 * Own capacity is normalized from the current legal Step-5 choices, so one Water with
 * several reroll/refresh targets, one Worm with several uses, and one Butterfly with
 * several target dice each remain one move. Opponent capacity uses only public board
 * state. Each hidden opponent Wisp counts as one potential move; its identity and exact
 * effect legality are never inferred.
 */
class BattleSupportCapacityAssessor {
    operator fun invoke(
        context: DecisionContext,
        legalChoices: Iterable<BattleTurnAction>,
        relevantLiveThreatOpponentIds: Set<PlayerId> = emptySet()
    ): BattleSupportCapacityAssessment {
        val battle = requireNotNull(context.battle) {
            "Battle Support capacity requires a Battle decision context"
        }
        val opponentIds = context.opponents.map { it.id }.toSet()
        require(relevantLiveThreatOpponentIds.all { it in opponentIds }) {
            "Relevant Live Threat IDs must identify visible opponents: " +
                relevantLiveThreatOpponentIds.filterNot { it in opponentIds }
        }
        require(relevantLiveThreatOpponentIds.none(battle::isDone)) {
            "Done opponents cannot be relevant Live Threats: " +
                relevantLiveThreatOpponentIds.filter(battle::isDone)
        }

        val own =
            if (battle.isDone(context.self.id)) {
                BattleSupportCapacity.ZERO
            } else {
                ownCapacity(context, legalChoices)
            }
        val opponents = context.opponents.associate { opponent ->
            opponent.id to
                if (battle.isDone(opponent.id)) {
                    BattleSupportCapacity.ZERO
                } else {
                    publicOpponentCapacity(context, opponent)
                }
        }
        val relevant = opponents.filterKeys { it in relevantLiveThreatOpponentIds }

        return BattleSupportCapacityAssessment(
            own = own,
            opponents = opponents,
            relevantLiveThreatOpponents = relevant
        )
    }

    private fun ownCapacity(
        context: DecisionContext,
        legalChoices: Iterable<BattleTurnAction>
    ): BattleSupportCapacity {
        val supports = legalChoices
            .filterIsInstance<BattleTurnAction.Support>()
            .map { it.action }
        val shared = supports
            .filterIsInstance<BattleSupportAction.Shared>()
            .map { it.action }
        val board = context.self.board
        val heldNormalWisps = context.self.wisps.count { !it.playImmediately }
        val usableMulch = board.mulch.count { it.storedDieSides != null }
        val faceUpButterflies = board.butterflies
            .filter { it.isFaceUp }
            .map { it.butterfly }
            .toSet()

        return BattleSupportCapacity(
            wispMoves = shared.count { it is SupportAction.PlayWisp }
                .coerceAtMost(heldNormalWisps),
            waterMoves = if (shared.any { it is SupportAction.UseWaterReroll || it == SupportAction.UseWaterRefresh }) {
                board.water
            } else {
                0
            },
            mulchMoves = shared.count { it is SupportAction.UseMulch }
                .coerceAtMost(usableMulch),
            wormMoves = if (
                shared.any { it is SupportAction.UseWormFlip } ||
                supports.any { it is BattleSupportAction.PlaceCritter && it.critter == Critter.WORM }
            ) {
                board.worms
            } else {
                0
            },
            beeMoves = if (
                supports.any { it is BattleSupportAction.PlaceCritter && it.critter == Critter.BEE }
            ) {
                board.bees
            } else {
                0
            },
            butterflyMoves = shared
                .filterIsInstance<SupportAction.UseButterfly>()
                .map { it.butterfly }
                .filter { it in faceUpButterflies }
                .distinct()
                .size
        )
    }

    private fun publicOpponentCapacity(
        context: DecisionContext,
        opponent: OpponentView
    ): BattleSupportCapacity {
        val battle = requireNotNull(context.battle)
        val rows = battle.rows.mapNotNull { row ->
            row.forPlayer(opponent.id)?.let { player -> row to player }
        }
        val hasOpenCritterRow = rows.any { (row, player) ->
            !row.closed && !player.withdrawn
        }
        val hasOpenDieSlot = rows.any { (row, player) ->
            !row.closed && !player.withdrawn && player.dice.size < BattleSquare.MAX_DICE
        }
        val hasBattleDie = rows.any { (_, player) -> player.dice.isNotEmpty() }
        val board = opponent.board

        return BattleSupportCapacity(
            // Identity is hidden. Each held Wisp is only one potential move.
            wispMoves = opponent.wispCount,
            waterMoves = board.water,
            mulchMoves = if (hasOpenDieSlot) {
                board.mulch.count { it.storedDieSides != null }
            } else {
                0
            },
            wormMoves = if (hasOpenCritterRow || board.creature.isNotEmpty()) {
                board.worms
            } else {
                0
            },
            beeMoves = if (hasOpenCritterRow) board.bees else 0,
            butterflyMoves = if (hasBattleDie) {
                board.butterflies.count { it.isFaceUp }
            } else {
                0
            }
        )
    }
}
