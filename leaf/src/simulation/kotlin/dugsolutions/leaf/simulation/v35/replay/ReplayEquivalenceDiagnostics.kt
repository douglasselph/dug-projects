package dugsolutions.leaf.simulation.v35.replay

import dugsolutions.leaf.v35.game.random.ReplayRollEvent
import dugsolutions.leaf.v35.game.random.ReplayRollKey

/** C5 reports only the events C2/C3 actually cover. Unkeyed RNG use is not counted. */
data class ReplayRollComparison(
    val matchedSameSize: Int,
    val matchedCrossSize: Int,
    val unexpectedMismatches: Int,
    val controlOnly: Int,
    val comparisonOnly: Int,
    val firstUnexpectedKey: ReplayRollKey?,
    val firstControlOnlyKey: ReplayRollKey?,
    val firstComparisonOnlyKey: ReplayRollKey?
)

data class ReplayDecisionComparison(
    val matched: Int,
    val changed: Int,
    val controlOnly: Int,
    val comparisonOnly: Int,
    val firstChanged: String?
)

data class ReplayEquivalenceReport(
    val exactDecisions: Boolean,
    val exactChronicle: Boolean,
    val exactSummary: Boolean,
    val decisions: ReplayDecisionComparison,
    val rolls: ReplayRollComparison
) {
    val exact: Boolean get() = exactDecisions && exactChronicle && exactSummary
}

object ReplayEquivalenceDiagnostics {
    fun compareRolls(control: List<ReplayRollEvent>, changed: List<ReplayRollEvent>): ReplayRollComparison {
        require(control.map { it.key }.distinct().size == control.size) { "Duplicate control roll keys" }
        require(changed.map { it.key }.distinct().size == changed.size) { "Duplicate comparison roll keys" }
        val c = control.associateBy { it.key }
        val d = changed.associateBy { it.key }
        val matching = c.keys intersect d.keys
        var sameSize = 0
        var crossSize = 0
        var mismatches = 0
        var firstMismatch: ReplayRollKey? = null
        for (key in matching) {
            val left = c.getValue(key)
            val right = d.getValue(key)
            val leftPercentile = left.percentile
            val rightPercentile = right.percentile
            val equalUnderlying = if (leftPercentile != null && rightPercentile != null) {
                leftPercentile == rightPercentile &&
                    left.face == (leftPercentile * left.sides).toInt() + 1 &&
                    right.face == (rightPercentile * right.sides).toInt() + 1
            } else leftPercentile == null && rightPercentile == null &&
                left.sides == right.sides && left.face == right.face
            if (!equalUnderlying) {
                mismatches++
                if (firstMismatch == null) firstMismatch = key
            } else if (left.sides == right.sides) sameSize++ else crossSize++
        }
        val onlyC = c.keys - d.keys
        val onlyD = d.keys - c.keys
        return ReplayRollComparison(sameSize, crossSize, mismatches, onlyC.size, onlyD.size,
            firstMismatch, onlyC.firstOrNull(), onlyD.firstOrNull())
    }

    fun compare(control: RecordedReplayGame, changed: RecordedReplayGame): ReplayEquivalenceReport {
        fun decisionKey(d: dugsolutions.leaf.v35.game.replay.CultivationReplayDecision) =
            Triple(d.roundNumber, d.playerId, d.consultationIndex)
        val c = control.decisions.associateBy(::decisionKey)
        val d = changed.decisions.associateBy(::decisionKey)
        var matches = 0
        var changes = 0
        var first: String? = null
        for ((key, a) in c) {
            val b = d[key] ?: continue
            if (a == b) matches++ else {
                changes++
                if (first == null) first = "round=${key.first},player=${key.second},consultation=${key.third}: ${a.actualChoice} -> ${b.actualChoice}"
            }
        }
        return ReplayEquivalenceReport(
            exactDecisions = control.decisions == changed.decisions,
            exactChronicle = control.chronicle == changed.chronicle,
            exactSummary = control.summary == changed.summary,
            decisions = ReplayDecisionComparison(matches, changes, (c.keys - d.keys).size,
                (d.keys - c.keys).size, first),
            rolls = compareRolls(control.rollEvents, changed.rollEvents)
        )
    }
}
