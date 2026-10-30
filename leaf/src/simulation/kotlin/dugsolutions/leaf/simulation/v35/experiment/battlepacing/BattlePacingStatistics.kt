package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.player.PlayerId
import kotlin.math.sqrt

/** Small pure-statistics layer; every paired difference is one complete Battle, not one player. */
class RunningMoments {
    var count: Int = 0
        private set
    var mean: Double = 0.0
        private set
    private var m2: Double = 0.0

    fun add(value: Double) {
        count++
        val delta = value - mean
        mean += delta / count
        m2 += delta * (value - mean)
    }

    val standardError: Double
        get() = if (count < 2) Double.NaN else sqrt(m2 / (count - 1) / count)

    /** Normal-approximation CI for paired means; insufficient N reports NaN. */
    fun interval95(): Pair<Double, Double> =
        if (count < 2) Double.NaN to Double.NaN else
            (mean - 1.96 * standardError) to (mean + 1.96 * standardError)
}

/** Wilson score 95% interval, robust near 0 and 1. */
fun wilson95(successes: Int, trials: Int): Pair<Double, Double> {
    require(trials >= 0 && successes in 0..trials)
    if (trials == 0) return Double.NaN to Double.NaN
    val z = 1.96
    val n = trials.toDouble()
    val p = successes / n
    val denom = 1 + z * z / n
    val center = (p + z * z / (2 * n)) / denom
    val margin = z * sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denom
    return (center - margin) to (center + margin)
}

/** Same tie rule as StrikeResolver; used ONLY for counterfactual last-turn reporting. */
fun projectedWinners(totals: Map<PlayerId, Int>): Set<PlayerId> {
    if (totals.isEmpty()) return emptySet()
    val best = totals.values.maxOrNull()!!
    if (totals.size > 1 && totals.values.all { it == best }) return emptySet()
    return totals.filterValues { it == best }.keys
}

/** Each observation concerns a single Candidate B Strike and its actual last actor. */
data class LastActorObservation(
    val hadAction: Boolean,
    val uniqueWinner: Boolean,
    val sharedWinner: Boolean,
    val swungIntoWin: Boolean
)

fun lastActorObservation(engagement: EngagementResult): LastActorObservation {
    val last = engagement.opportunities.last()
    val actual = engagement.resolution.winnerIds.toSet()
    val before = projectedWinners(last.beforeTotals)
    return LastActorObservation(
        hadAction = last.actions.isNotEmpty(),
        uniqueWinner = actual.size == 1 && last.playerId in actual,
        sharedWinner = actual.size > 1 && last.playerId in actual,
        swungIntoWin = last.actions.isNotEmpty() && last.playerId !in before && last.playerId in actual
    )
}

/** Cohort is one fixed player count, Battle ordinal and policy condition. */
class BattlePacingCohortSummary {
    val pairedVpPerPlayer = RunningMoments()
    val pairedStrikeVpPerPlayer = RunningMoments()
    val pairedWoundsPerPlayer = RunningMoments()
    val lastUniqueSharePerBattle = RunningMoments()
    val baselineBattleVpPerPlayer = RunningMoments()
    val candidateBattleVpPerPlayer = RunningMoments()
    var strikes: Int = 0
        private set
    var lastUniqueWins: Int = 0
        private set
    var lastSharedWins: Int = 0
        private set
    var lastActorSwings: Int = 0
        private set
    var lastActorActs: Int = 0
        private set
    var baselineMainActions: Int = 0
        private set
    var baselineSupportActions: Int = 0
        private set
    var candidateMainActions: Int = 0
        private set
    var candidateSupportActions: Int = 0
        private set
    var candidateEmptyOpportunities: Int = 0
        private set
    var candidateOpportunities: Int = 0
        private set

    fun add(result: PairedBattleComparisonResult, startingVP: List<Int>) {
        val n = startingVP.size
        check(result.baselineFinalVP.size == n && result.candidateFinalVP.size == n)
        fun average(values: List<Int>): Double = values.sum().toDouble() / n
        val aStrike = result.baselineStrikes.strikes.sumOf { it.winnerIds.size * it.vpPerWinner }
        val bStrike = result.candidate.engagements.sumOf { it.resolution.winnerIds.size * it.resolution.vpPerWinner }
        val aWounds = result.baselineStrikes.strikes.sumOf { it.wounds.size }
        val bWounds = result.candidate.engagements.sumOf { it.resolution.wounds.size }
        val aVP = average(result.baselineFinalVP.zip(startingVP) { end, start -> end - start })
        val bVP = average(result.candidateFinalVP.zip(startingVP) { end, start -> end - start })
        baselineBattleVpPerPlayer.add(aVP)
        candidateBattleVpPerPlayer.add(bVP)
        pairedVpPerPlayer.add(bVP - aVP)
        pairedStrikeVpPerPlayer.add((bStrike - aStrike).toDouble() / n)
        pairedWoundsPerPlayer.add((bWounds - aWounds).toDouble() / n)
        baselineMainActions += result.baselineActions.firstMainActions.size + result.baselineActions.finalMainActions.size
        baselineSupportActions += result.baselineActions.supportActions.size
        val uniqueBefore = lastUniqueWins
        result.candidate.engagements.forEach { engagement ->
            strikes++
            val last = lastActorObservation(engagement)
            if (last.uniqueWinner) lastUniqueWins++
            if (last.sharedWinner) lastSharedWins++
            if (last.swungIntoWin) lastActorSwings++
            if (last.hadAction) lastActorActs++
            engagement.opportunities.forEach { opportunity ->
                candidateOpportunities++
                if (opportunity.actions.isEmpty()) candidateEmptyOpportunities++
            }
            engagement.actions.forEach { record ->
                when (record.choice) {
                    is EngagementChoice.Main -> candidateMainActions++
                    is EngagementChoice.Support -> candidateSupportActions++
                    EngagementChoice.Pass -> Unit
                }
            }
        }
        lastUniqueSharePerBattle.add((lastUniqueWins - uniqueBefore).toDouble() / result.candidate.engagements.size)
    }
}
