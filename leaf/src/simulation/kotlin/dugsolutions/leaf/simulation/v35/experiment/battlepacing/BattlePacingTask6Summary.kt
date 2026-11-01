package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import kotlin.math.ceil

internal fun percentile(values: List<Double>, probability: Double): Double {
    require(probability in 0.0..1.0)
    if (values.isEmpty()) return Double.NaN
    val sorted = values.sorted()
    val rank = ceil(probability * sorted.size).toInt().coerceIn(1, sorted.size)
    return sorted[rank - 1]
}

internal fun median(values: List<Double>): Double {
    if (values.isEmpty()) return Double.NaN
    val s = values.sorted()
    val mid = s.size / 2
    return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2.0
}

internal fun mean(values: List<Double>): Double = if (values.isEmpty()) Double.NaN else values.average()

internal class Task6CohortSummary {
    val aDuration = mutableListOf<Double>()
    val bDuration = mutableListOf<Double>()
    val savings = mutableListOf<Double>()
    val savingsPct = mutableListOf<Double>()
    val aWait = mutableListOf<Double>()
    val bWait = mutableListOf<Double>()
    val aLongest = mutableListOf<Double>()
    val bLongest = mutableListOf<Double>()
    val aTerminal = mutableListOf<Double>()
    val bTerminal = mutableListOf<Double>()
    val aNoLegal = mutableListOf<Double>()
    val bNoLegal = mutableListOf<Double>()
    val aWatching = mutableListOf<Double>()
    val bWatching = mutableListOf<Double>()
    val breakEvenPlanning = mutableListOf<Double>()
    val pairedDurationDelta = RunningMoments()
    val pairedDecisionWindows = RunningMoments()
    val pairedHandoffs = RunningMoments()
    val pairedSupports = RunningMoments()
    val pairedUnusedPerPlayer = RunningMoments()
    val aDecisionWindows = mutableListOf<Double>()
    val bDecisionWindows = mutableListOf<Double>()
    val aMeaningfulWindows = mutableListOf<Double>()
    val bMeaningfulWindows = mutableListOf<Double>()
    val aHandoffs = mutableListOf<Double>()
    val bHandoffs = mutableListOf<Double>()
    val aSupports = mutableListOf<Double>()
    val bSupports = mutableListOf<Double>()
    val aUnused = mutableListOf<Double>()
    val bUnused = mutableListOf<Double>()

    fun add(a: BattlePacingObserved, b: BattlePacingObserved, ae: BattleTimeEstimate, be: BattleTimeEstimate) {
        aDuration += ae.totalSeconds; bDuration += be.totalSeconds
        val save = ae.totalSeconds - be.totalSeconds
        savings += save; savingsPct += if (ae.totalSeconds == 0.0) 0.0 else 100.0 * save / ae.totalSeconds
        pairedDurationDelta.add(be.totalSeconds - ae.totalSeconds)
        aWait += ae.playerDowntime.map { it.totalWaitingSeconds }; bWait += be.playerDowntime.map { it.totalWaitingSeconds }
        aLongest += ae.playerDowntime.map { it.longestWaitingSeconds }; bLongest += be.playerDowntime.map { it.longestWaitingSeconds }
        aTerminal += ae.playerDowntime.map { it.terminalWaitingSeconds }; bTerminal += be.playerDowntime.map { it.terminalWaitingSeconds }
        aNoLegal += ae.playerDowntime.map { it.noMeaningfulLegalActionSeconds }; bNoLegal += be.playerDowntime.map { it.noMeaningfulLegalActionSeconds }
        aWatching += ae.playerDowntime.map { it.consequentialWatchingSeconds }; bWatching += be.playerDowntime.map { it.consequentialWatchingSeconds }
        if (be.candidateLaterStrikeUnits > 0) breakEvenPlanning += save / be.candidateLaterStrikeUnits
        pairedDecisionWindows.add((b.decisionWindows - a.decisionWindows).toDouble())
        pairedHandoffs.add((b.handoffs - a.handoffs).toDouble())
        pairedSupports.add((b.supportActions - a.supportActions).toDouble())
        val au = a.remainingSupportInventory.values.average(); val bu = b.remainingSupportInventory.values.average()
        pairedUnusedPerPlayer.add(bu - au)
        aDecisionWindows += a.decisionWindows.toDouble(); bDecisionWindows += b.decisionWindows.toDouble()
        aMeaningfulWindows += a.meaningfulDecisionWindows.toDouble(); bMeaningfulWindows += b.meaningfulDecisionWindows.toDouble()
        aHandoffs += a.handoffs.toDouble(); bHandoffs += b.handoffs.toDouble()
        aSupports += a.supportActions.toDouble(); bSupports += b.supportActions.toDouble()
        aUnused += au; bUnused += bu
    }
}
