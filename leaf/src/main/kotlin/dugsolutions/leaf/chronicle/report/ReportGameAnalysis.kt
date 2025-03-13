package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.AnalysisResults
import dugsolutions.leaf.chronicle.domain.DominancePeriod
import dugsolutions.leaf.chronicle.domain.EventTurn
import dugsolutions.leaf.chronicle.domain.Finished
import dugsolutions.leaf.chronicle.domain.EventBattle
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.chronicle.domain.TurnData

class ReportGameAnalysis(
    private val chronicle: GameChronicle
) {

    operator fun invoke(): List<String> {
        val entries = chronicle.getEntries()
        val turnEntries = entries.filterIsInstance<EventTurn>()
        val finishedEntry = entries.filterIsInstance<Finished>().firstOrNull()
        val battleEntry = entries.filterIsInstance<EventBattle>().firstOrNull()

        if (turnEntries.isEmpty()) {
            return listOf("Not enough data for analysis")
        }
        // Process all turn entries including the battle and finished entries if available
        val allTurnData = mutableListOf<TurnData>()

        // Process EventTurn entries
        turnEntries.forEach { entry ->
            allTurnData.add(processTurnEntry(entry))
        }

        // Process Battle entry if available - but only if it's not duplicating a turn we already processed
        battleEntry?.let { entry ->
            // Check if we already have an entry for this turn
            val turnExists = allTurnData.any { it.turn == entry.turn }

            if (turnExists) {
                // If we already have this turn, just mark it as the battle begin turn
                val updatedData = allTurnData.toMutableList()
                for (i in updatedData.indices) {
                    if (updatedData[i].turn == entry.turn) {
                        updatedData[i] = updatedData[i].copy(isBattleBegin = true)
                    }
                }
                allTurnData.clear()
                allTurnData.addAll(updatedData)
            } else {
                // If no entry exists for this turn, add a new one
                allTurnData.add(processBattleEntry(entry))
            }
        }

        // Process Finished entry if available
        finishedEntry?.let { entry ->
            allTurnData.add(processFinishedEntry(entry))
        }

        // Sort by turn number to ensure chronological order
        val turnDataList = allTurnData.sortedBy { it.turn }

        // Get the winner from the final scores
        val winner = turnDataList.last().leadPlayerId

        // Find the battle begin turn if it exists
        val battleBeginTurn = turnDataList.find { it.isBattleBegin }?.turn

        // Calculate metrics
        val results = analyze(turnDataList, winner, battleBeginTurn)
        
        // Generate report
        return generateReport(results, turnDataList)
    }
    
    private fun processTurnEntry(entry: EventTurn): TurnData {
        return processScores(entry.turn, entry.scores.map { score ->
            PlayerScore(
                playerId = score.data.playerId,
                scoreDice = score.data.scoreDice,
                scoreCards = score.data.scoreCards
            )
        })
    }
    
    private fun processBattleEntry(entry: EventBattle): TurnData {
        // Battle entry may have scores or may just be a marker
        val playerScores = entry.scores.map { score ->
            PlayerScore(
                playerId = score.data.playerId,
                scoreDice = score.data.scoreDice,
                scoreCards = score.data.scoreCards
            )
        }
        return processScores(entry.turn, playerScores, isBattleBegin = true)
    }
    
    private fun processFinishedEntry(entry: Finished): TurnData {
        // For a finished entry, we need to extract players' final scores
        // This assumes that your Finished entry has a similar structure as EventTurn
        // with a scores field that contains the final state of all players
        
        // If Finished doesn't have scores directly, you might need to adapt this
        // to extract the necessary data from the reports or other fields
        val playerScores = entry.scores.map { score ->
            PlayerScore(
                playerId = score.data.playerId,
                scoreDice = score.data.scoreDice,
                scoreCards = score.data.scoreCards
            )
        }
        return processScores(entry.turn, playerScores)
    }
    
    private fun processScores(
        turn: Int, 
        playerScores: List<PlayerScore>,
        isBattleBegin: Boolean = false
    ): TurnData {
        // Find highest and second highest scoring players
        val sortedScores = playerScores.sortedByDescending { it.scoreDice }
        val leadPlayer = sortedScores.firstOrNull() ?: 
            return TurnData(
                turn = turn, 
                scores = playerScores, 
                leadPlayerId = 0,
                isBattleBegin = isBattleBegin
            )
            
        val secondPlayer = if (sortedScores.size > 1) sortedScores[1] else null
        val gap = if (secondPlayer != null) leadPlayer.scoreDice - secondPlayer.scoreDice else 0
        
        return TurnData(
            turn = turn,
            scores = playerScores,
            leadPlayerId = leadPlayer.playerId,
            secondPlayerId = secondPlayer?.playerId,
            gap = gap,
            isBattleBegin = isBattleBegin
        )
    }
    
    private fun analyze(
        turnDataList: List<TurnData>, 
        winnerId: Int, 
        battleBeginTurn: Int?
    ): AnalysisResults {
        // Find the widest gap
        val widestGapTurn = turnDataList.maxByOrNull { it.gap } ?: turnDataList.first()
        val widestGap = widestGapTurn.gap
        
        // Calculate lead changes in cultivation and battle phases separately
        var totalLeadChanges = 0
        var cultivationLeadChanges = 0
        var battleLeadChanges = 0
        
        var previousLeader = turnDataList.firstOrNull()?.leadPlayerId ?: 0
        
        val dominancePeriods = mutableListOf<DominancePeriod>()
        var currentDominancePeriodStart = turnDataList.firstOrNull()?.turn ?: 1
        var currentDominantPlayer = previousLeader
        
        turnDataList.forEachIndexed { index, turnData ->
            if (turnData.leadPlayerId != previousLeader) {
                // Record end of previous dominance period
                dominancePeriods.add(
                    DominancePeriod(
                        playerId = previousLeader,
                        startTurn = currentDominancePeriodStart,
                        endTurn = turnData.turn - 1,
                        duration = turnData.turn - currentDominancePeriodStart
                    )
                )
                
                // Start new dominance period
                currentDominancePeriodStart = turnData.turn
                currentDominantPlayer = turnData.leadPlayerId
                
                // Count lead changes and categorize by phase
                totalLeadChanges++
                
                // Determine if this change happened during battle phase
                if (battleBeginTurn != null && turnData.turn >= battleBeginTurn) {
                    battleLeadChanges++
                } else {
                    cultivationLeadChanges++
                }
                
                previousLeader = turnData.leadPlayerId
            }
        }
        
        // Add the final dominance period
        val lastTurn = turnDataList.last().turn
        dominancePeriods.add(
            DominancePeriod(
                playerId = currentDominantPlayer,
                startTurn = currentDominancePeriodStart,
                endTurn = lastTurn,
                duration = lastTurn - currentDominancePeriodStart + 1
            )
        )
        
        // Determine if winner was initially dominated
        val initialLeader = turnDataList.first().leadPlayerId
        val winnerInitiallyDominated = winnerId != initialLeader
        
        // Determine if this was a comeback win
        val comebackWin = winnerInitiallyDominated && 
                dominancePeriods.last().playerId == winnerId && 
                dominancePeriods.size > 1
        
        return AnalysisResults(
            widestGap = widestGap,
            widestGapTurn = widestGapTurn.turn,
            leadChanges = totalLeadChanges,
            cultivationLeadChanges = cultivationLeadChanges,
            battleLeadChanges = battleLeadChanges,
            dominancePeriods = dominancePeriods,
            finalScores = turnDataList.last().scores,
            winner = winnerId,
            winnerInitiallyDominated = winnerInitiallyDominated,
            comebackWin = comebackWin,
            battleBeginTurn = battleBeginTurn
        )
    }
    
    private fun generateReport(results: AnalysisResults, turnDataList: List<TurnData>): List<String> {
        val report = mutableListOf<String>()
        
        // General game stats
        report.add("Game Analysis:")
        report.add("Total Turns: ${turnDataList.last().turn}")
        report.add("Winner: Player ${results.winner}")
        
        // Battle phase information
        results.battleBeginTurn?.let {
            report.add("Battle Phase began on Turn $it")
        }
        
        // Score gap analysis
        report.add("Widest Score Gap: ${results.widestGap} (Turn ${results.widestGapTurn})")
        
        // Lead changes - now separated by phase
        report.add("Lead Changes (Total): ${results.leadChanges}")
        report.add("  Cultivation Phase: ${results.cultivationLeadChanges}")
        report.add("  Battle Phase: ${results.battleLeadChanges}")
        
        // Dominance periods
        report.add("Dominance Periods:")
        results.dominancePeriods.forEach { period ->
            if (period.startTurn == period.endTurn) {
                report.add("  Player ${period.playerId}: Turn ${period.startTurn}")
            } else {
                report.add("  Player ${period.playerId}: Turns ${period.startTurn}-${period.endTurn} (${period.duration} turns)")
            }
        }
        
        // Special cases
        if (results.comebackWin) {
            report.add("COMEBACK WIN: Player ${results.winner} started behind but came back to win!")
        }
        
        if (results.leadChanges == 0) {
            report.add("COMPLETE DOMINANCE: The leading player never changed throughout the game.")
        }
        
        // Final scores
        report.add("Final Scores:")
        results.finalScores.sortedByDescending { it.scoreDice }.forEach { score ->
            report.add("  Player ${score.playerId}: Dice=${score.scoreDice}, Cards=${score.scoreCards}")
        }
        
        // Add turn-by-turn score graph for 2 players
        if (turnDataList.any { it.scores.size >= 2 }) {
            // Get the winner ID and find any other player ID (for 2-player comparison)
            val winnerId = results.winner
            val otherPlayerId = turnDataList.first().scores
                .map { it.playerId }
                .firstOrNull { it != winnerId } ?: return report

            report.add("\nTurn-by-Turn Score Progression:")
            report.add("Turn | Winner P${winnerId} | Player P${otherPlayerId} | Gap | Leader")
            report.add("-".repeat(50))
            
            turnDataList.forEach { turn ->
                // Check if this is the battle begin turn
                if (turn.isBattleBegin) {
                    report.add("\n--------- BATTLE BEGINS ---------\n")
                }
                
                // Only add the turn data row if it has scores
                if (turn.scores.isNotEmpty()) {
                    val winnerScore = turn.scores.find { it.playerId == winnerId }?.scoreDice ?: 0
                    val otherScore = turn.scores.find { it.playerId == otherPlayerId }?.scoreDice ?: 0
                    val gap = Math.abs(winnerScore - otherScore)
                    val leader = if (winnerScore > otherScore) winnerId else otherPlayerId
                    
                    report.add(String.format("%-4d | %-12d | %-12d | %-3d | Player %d", 
                        turn.turn, winnerScore, otherScore, gap, leader))
                }
            }
        }
        
        return report
    }
}
