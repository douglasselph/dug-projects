package dugsolutions.leaf.chronicle.domain

import dugsolutions.leaf.player.domain.PlayerScore

data class TurnData(
    val turn: Int,
    val scores: List<PlayerScore>,
    val leadPlayerId: Int,
    val secondPlayerId: Int? = null,
    val gap: Int = 0,
    val isBattleBegin: Boolean = false
)

data class AnalysisResults(
    val widestGap: Int,
    val widestGapTurn: Int,
    val leadChanges: Int,
    val cultivationLeadChanges: Int,
    val battleLeadChanges: Int,
    val dominancePeriods: List<DominancePeriod>,
    val finalScores: List<PlayerScore>,
    val winner: Int,
    val winnerInitiallyDominated: Boolean,
    val comebackWin: Boolean,
    val battleBeginTurn: Int? = null
)

data class DominancePeriod(
    val playerId: Int,
    val startTurn: Int,
    val endTurn: Int,
    val duration: Int
)
