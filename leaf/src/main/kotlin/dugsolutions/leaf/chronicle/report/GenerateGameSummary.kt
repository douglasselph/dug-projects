package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.EventBattle
import dugsolutions.leaf.chronicle.domain.EventTurn
import dugsolutions.leaf.chronicle.domain.GameSummary
import dugsolutions.leaf.chronicle.domain.OrderingEntry
import dugsolutions.leaf.chronicle.domain.PlayerUnderTest

class GenerateGameSummary(
    private val chronicle: GameChronicle,
    private val playerUnderTest: PlayerUnderTest
) {

    operator fun invoke(): GameSummary {
        val entries = chronicle.getEntries()
        val turnEntries = entries.filterIsInstance<EventTurn>()
        val battleEntry = entries.filterIsInstance<EventBattle>().firstOrNull()
        val orderingEntry = entries.filterIsInstance<OrderingEntry>().firstOrNull()
        
        // Get the player ID who had the first move (if available)
        val playerIdOfFirstPlayer = if (orderingEntry != null && orderingEntry.playerOrder.isNotEmpty()) {
            orderingEntry.playerOrder.first()
        } else {
            -1 // No player order information available
        }
        
        if (turnEntries.isEmpty()) {
            return GameSummary(
                totalTurns = 0,
                playerIdUnderTest = playerUnderTest.playerId,
                placeDistribution = emptyList(), // No players, empty distribution
                playerIdOfFirstPlayer = playerIdOfFirstPlayer,
                battleTransitionOnTurn = 0,
                narrowestBattleGap = 0,
                widestBattleGap = 0,
                numberOfFlips = 0,
                numberOfCultivationFlips = 0,
                numberOfBattleFlips = 0,
                maxScore = 0,
                averageGapChange = 0,
                largestGapChange = 0
            )
        }
        
        // Total turns is the last turn number
        val totalTurns = turnEntries.maxByOrNull { it.turn }?.turn ?: 0
        
        // Battle transition turn
        val battleTransitionOnTurn = battleEntry?.turn ?: 0
        
        // Process all turn entries to calculate score metrics
        val turnScoreData = processTurnEntries(turnEntries, battleTransitionOnTurn)
        
        // Metrics that require battle phase data
        val (narrowestBattleGap, widestBattleGap) = calculateBattleGaps(turnScoreData, battleTransitionOnTurn)
        
        // Calculate the number of lead changes (flips)
        val (totalFlips, cultivationFlips, battleFlips) = calculateNumberOfFlips(turnScoreData)
        
        // Find the maximum score reached by any player
        val maxScore = turnScoreData.flatMap { it.playerScores }.maxOfOrNull { it.score } ?: 0
        
        // Calculate gap changes
        val (averageGapChange, largestGapChange) = calculateGapChanges(turnScoreData)
        
        // Calculate final player placement as a list where 
        // index = place (0 = first, 1 = second, etc.) and value = player ID
        val placeDistribution = calculatePlayerPlacement(turnScoreData)
        
        return GameSummary(
            totalTurns = totalTurns,
            playerIdUnderTest = playerUnderTest.playerId,
            placeDistribution = placeDistribution,
            playerIdOfFirstPlayer = playerIdOfFirstPlayer,
            battleTransitionOnTurn = battleTransitionOnTurn,
            narrowestBattleGap = narrowestBattleGap,
            widestBattleGap = widestBattleGap,
            numberOfFlips = totalFlips,
            numberOfCultivationFlips = cultivationFlips,
            numberOfBattleFlips = battleFlips,
            maxScore = maxScore,
            averageGapChange = averageGapChange,
            largestGapChange = largestGapChange
        )
    }
    
    private data class PlayerScoreData(val playerId: Int, val score: Int)
    
    private data class TurnScoreData(
        val turn: Int, 
        val playerScores: List<PlayerScoreData>,
        val leadingPlayerId: Int,
        val gap: Int,
        val isBattlePhase: Boolean
    )
    
    /**
     * Calculate the final placement of all players
     * Returns a list where the index is the place (0 = first) and the value is the player ID
     */
    private fun calculatePlayerPlacement(turnScoreData: List<TurnScoreData>): List<Int> {
        if (turnScoreData.isEmpty()) return emptyList()
        
        // Get the last turn's data
        val lastTurnData = turnScoreData.maxByOrNull { it.turn } ?: return emptyList()
        
        // Sort players by scores in descending order
        val sortedPlayers = lastTurnData.playerScores.sortedByDescending { it.score }
        
        // Create a list of player IDs in order of their placement
        return sortedPlayers.map { it.playerId }
    }
    
    private fun processTurnEntries(
        turnEntries: List<EventTurn>, 
        battleTransitionTurn: Int
    ): List<TurnScoreData> {
        val scoreDataList = mutableListOf<TurnScoreData>()
        
        turnEntries.forEach { entry ->
            val playerScores = entry.scores.map { score ->
                PlayerScoreData(
                    playerId = score.data.playerId,
                    score = score.data.scoreDice // Using dice score as the primary score
                )
            }
            
            // Sort scores to find the leader and calculate the gap
            val sortedScores = playerScores.sortedByDescending { it.score }
            val leadingPlayerId = sortedScores.firstOrNull()?.playerId ?: 0
            val gap = if (sortedScores.size >= 2) {
                sortedScores[0].score - sortedScores[1].score
            } else {
                0
            }
            
            scoreDataList.add(
                TurnScoreData(
                    turn = entry.turn,
                    playerScores = playerScores,
                    leadingPlayerId = leadingPlayerId,
                    gap = gap,
                    isBattlePhase = entry.turn >= battleTransitionTurn && battleTransitionTurn > 0
                )
            )
        }
        
        return scoreDataList.sortedBy { it.turn }
    }
    
    private fun calculateBattleGaps(
        turnScoreData: List<TurnScoreData>, 
        battleTransitionTurn: Int
    ): Pair<Int, Int> {
        // If there is no battle phase, return 0 for both
        if (battleTransitionTurn <= 0) return Pair(0, 0)
        
        val battlePhaseData = turnScoreData.filter { it.isBattlePhase }
        if (battlePhaseData.isEmpty()) return Pair(0, 0)
        
        val narrowestGap = battlePhaseData.minOfOrNull { it.gap } ?: 0
        val widestGap = battlePhaseData.maxOfOrNull { it.gap } ?: 0
        
        return Pair(narrowestGap, widestGap)
    }
    
    private fun calculateNumberOfFlips(
        turnScoreData: List<TurnScoreData>
    ): Triple<Int, Int, Int> {
        if (turnScoreData.size <= 1) return Triple(0, 0, 0)
        
        var totalFlips = 0
        var cultivationFlips = 0
        var battleFlips = 0
        
        var previousLeader = turnScoreData.first().leadingPlayerId
        
        for (i in 1 until turnScoreData.size) {
            val currentData = turnScoreData[i]
            val currentLeader = currentData.leadingPlayerId
            
            if (currentLeader != previousLeader && currentLeader != 0) {
                // Count total flips
                totalFlips++
                
                // Determine which phase this flip occurred in
                if (currentData.isBattlePhase) {
                    battleFlips++
                } else {
                    cultivationFlips++
                }
                
                previousLeader = currentLeader
            }
        }
        
        return Triple(totalFlips, cultivationFlips, battleFlips)
    }
    
    private fun calculateGapChanges(turnScoreData: List<TurnScoreData>): Pair<Int, Int> {
        if (turnScoreData.size <= 1) return Pair(0, 0)
        
        val gapChanges = mutableListOf<Int>()
        
        for (i in 1 until turnScoreData.size) {
            val previousGap = turnScoreData[i-1].gap
            val currentGap = turnScoreData[i].gap
            val change = kotlin.math.abs(currentGap - previousGap)
            gapChanges.add(change)
        }
        
        val largestChange = gapChanges.maxOrNull() ?: 0
        val averageChange = if (gapChanges.isNotEmpty()) {
            gapChanges.sum() / gapChanges.size
        } else {
            0
        }
        
        return Pair(averageChange, largestChange)
    }
}
