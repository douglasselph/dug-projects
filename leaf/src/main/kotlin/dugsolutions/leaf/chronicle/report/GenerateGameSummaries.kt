package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.domain.GameSummaries
import dugsolutions.leaf.chronicle.domain.GameSummary
import dugsolutions.leaf.chronicle.domain.Summary
import kotlin.math.sqrt

class GenerateGameSummaries {

    operator fun invoke(summaries: List<GameSummary>): GameSummaries {
        if (summaries.isEmpty()) {
            return createEmptySummaries()
        }

        // Calculate number of wins for player under test
        val numberOfWinsPlayerUnderTest = summaries.count { summary ->
            // Find place of player under test from placeDistribution
            val playerPosition = getPlayerPosition(summary.placeDistribution, summary.playerIdUnderTest)
            // Player won if they are in first place (position 0)
            playerPosition == 0
        }
        
        // Calculate number of times the player who went first also won
        val playerWhoWonWasFirst = summaries.count { summary ->
            // If the player who went first won, they must be the first entry in placeDistribution
            summary.playerIdOfFirstPlayer != -1 && 
            summary.placeDistribution.isNotEmpty() &&
            summary.placeDistribution[0] == summary.playerIdOfFirstPlayer
        }
        
        // Calculate place distribution for player under test
        // Find max place + 1 to account for 0-based indexing
        val maxPlace = summaries.mapNotNull { summary -> 
            getPlayerPosition(summary.placeDistribution, summary.playerIdUnderTest)
        }.maxOrNull()?.plus(1) ?: 0
        
        val placeDistribution = MutableList(maxPlace) { 0 }
        summaries.forEach { summary ->
            val place = getPlayerPosition(summary.placeDistribution, summary.playerIdUnderTest)
            if (place >= 0 && place < maxPlace) {
                placeDistribution[place]++
            }
        }
        
        return GameSummaries(
            numberOfGames = summaries.size,
            numberOfWinsPlayerUnderTest = numberOfWinsPlayerUnderTest,
            placeDistributionPlayerUnderTest = placeDistribution,
            playerWhoWonWasFirst = playerWhoWonWasFirst,
            numberOfTurns = calculateSummary(summaries) { it.totalTurns },
            battleTransition = calculateSummary(summaries) { it.battleTransitionOnTurn },
            narrowestBattleGap = calculateSummary(summaries) { it.narrowestBattleGap },
            widestBattleGap = calculateSummary(summaries) { it.widestBattleGap },
            numberOfFlips = calculateSummary(summaries) { it.numberOfFlips },
            numberOfCultivationFlips = calculateSummary(summaries) { it.numberOfCultivationFlips },
            numberOfBattleFlips = calculateSummary(summaries) { it.numberOfBattleFlips },
            maxScore = calculateSummary(summaries) { it.maxScore },
            averageGapChange = calculateSummary(summaries) { it.averageGapChange },
            largestGapChange = calculateSummary(summaries) { it.largestGapChange }
        )
    }
    
    /**
     * Get the position of a player in the place distribution
     * 
     * @param placeDistribution List of player IDs ordered by place (index 0 = first place)
     * @param playerId The player ID to find
     * @return The position (0 = first place, 1 = second place, etc.) or -1 if not found
     */
    private fun getPlayerPosition(placeDistribution: List<Int>, playerId: Int): Int {
        return placeDistribution.indexOf(playerId)
    }
    
    /**
     * Calculate statistical summary for a specific property across all GameSummary objects
     */
    private fun calculateSummary(summaries: List<GameSummary>, selector: (GameSummary) -> Int): Summary {
        val values = summaries.map(selector)
        
        val largest = values.maxOrNull() ?: 0
        val smallest = values.minOrNull() ?: 0
        val average = if (values.isNotEmpty()) values.sum() / values.size else 0
        val stdDev = calculateStandardDeviation(values, average)
        
        return Summary(
            largest = largest,
            smallest = smallest,
            average = average,
            standardDeviation = stdDev
        )
    }
    
    /**
     * Calculate the standard deviation of a list of integers
     */
    private fun calculateStandardDeviation(values: List<Int>, mean: Int): Int {
        if (values.size <= 1) return 0
        
        val variance = values.fold(0.0) { acc, value ->
            acc + (value - mean).toDouble().pow(2)
        } / values.size
        
        return sqrt(variance).toInt()
    }
    
    /**
     * Create an empty GameSummaries object with all zeros
     */
    private fun createEmptySummaries(): GameSummaries {
        val emptySummary = Summary(
            largest = 0,
            smallest = 0,
            average = 0,
            standardDeviation = 0
        )
        
        return GameSummaries(
            numberOfGames = 0,
            numberOfWinsPlayerUnderTest = 0,
            placeDistributionPlayerUnderTest = emptyList(),
            playerWhoWonWasFirst = 0,
            numberOfTurns = emptySummary,
            battleTransition = emptySummary,
            narrowestBattleGap = emptySummary,
            widestBattleGap = emptySummary,
            numberOfFlips = emptySummary,
            numberOfCultivationFlips = emptySummary,
            numberOfBattleFlips = emptySummary,
            maxScore = emptySummary,
            averageGapChange = emptySummary,
            largestGapChange = emptySummary
        )
    }
    
    /**
     * Extension function to calculate power of a Double
     */
    private fun Double.pow(exponent: Int): Double {
        var result = 1.0
        repeat(exponent) {
            result *= this
        }
        return result
    }
}
