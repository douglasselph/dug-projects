package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId

/**
 * Minimal row facts needed to project the actor's Strike VP.
 *
 * The real Strike rule awards every winner 2 VP plus 1 VP for each opponent
 * wounded in that row. Doubles allow the same calculation to be reused for
 * expected-value projections without rounding a die expectation early.
 */
data class BattleVpState(
    val row: StrikeRow,
    val available: Boolean,
    val currentlyWinning: Boolean,
    val ownTotal: Double,
    val opponentTotals: Map<PlayerId, Double>
) {
    companion object {
        fun from(assessment: BattleRowAssessment): BattleVpState =
            BattleVpState(
                row = assessment.row,
                available = assessment.available,
                currentlyWinning = assessment.currentlyWinning,
                ownTotal = assessment.ownTotal.toDouble(),
                opponentTotals = assessment.participatingOpponentTotals
                    .mapValues { (_, total) -> total.toDouble() }
            )
    }
}

/** Strike-VP change for one affected row. */
data class BattleRowVpImpact(
    val row: StrikeRow,
    val beforeVp: Int,
    val afterVp: Int
) {
    val gain: Int = afterVp - beforeVp
}

/** Aggregate Strike-VP impact of one immediate multi-row realization. */
data class BattleVpImpactResult(
    val rowImpacts: List<BattleRowVpImpact>
) {
    val beforeVp: Int = rowImpacts.sumOf { it.beforeVp }
    val afterVp: Int = rowImpacts.sumOf { it.afterVp }
    val gain: Int = afterVp - beforeVp
}

/**
 * Projects the actor's immediate Strike VP before and after one candidate result.
 *
 * This is intentionally separate from [BattleSwingEvaluator]. Battle Swing is the
 * richer tactical value used to compare immediate board changes; Strike-VP impact
 * is the simpler gate later used for expensive/cumulative Support commitments.
 *
 * The calculation mirrors `StrikeResolver`: a winner earns 2 VP plus one additional
 * VP for each participating opponent trailing the actor by at least 5. Shared winners
 * therefore receive the same wound bonus from lower players, while an all-player tie
 * (represented by `currentlyWinning == false`) earns 0 VP.
 */
class BattleVpImpact {
    operator fun invoke(
        before: BattleRowAssessment,
        after: BattleRowAssessment
    ): BattleRowVpImpact =
        invoke(BattleVpState.from(before), BattleVpState.from(after))

    operator fun invoke(
        before: BattleVpState,
        after: BattleVpState
    ): BattleRowVpImpact {
        require(before.row == after.row) {
            "Battle VP impact must compare the same row: ${before.row} vs ${after.row}"
        }
        return BattleRowVpImpact(
            row = before.row,
            beforeVp = vp(before),
            afterVp = vp(after)
        )
    }

    operator fun invoke(
        changes: Iterable<BattleActionRowChange>
    ): BattleVpImpactResult =
        BattleVpImpactResult(
            rowImpacts = changes.map { change ->
                invoke(change.before.toVpState(), change.after.toVpState())
            }
        )

    private fun vp(state: BattleVpState): Int {
        if (!state.available || !state.currentlyWinning) return 0
        val wounds = state.opponentTotals.values.count { opponentTotal ->
            state.ownTotal - opponentTotal >= WOUND_MARGIN - EPSILON
        }
        return BASE_STRIKE_VP + wounds
    }

    private companion object {
        const val BASE_STRIKE_VP = 2
        const val WOUND_MARGIN = 5.0
        const val EPSILON = 1e-9
    }
}
