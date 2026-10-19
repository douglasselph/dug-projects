package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.domain.GameSummaries
import dugsolutions.leaf.chronicle.domain.Summary

class ReportGameSummaries {

    operator fun invoke(summaries: GameSummaries): List<String> {
        val result = mutableListOf<String>()
        
        result.add("== Game Summaries Report ==")
        result.add("Games Analyzed: ${summaries.numberOfGames}")
        
        if (summaries.numberOfGames > 0) {
            // Report player under test statistics
            result.add("Player Under Test Statistics:")
            result.add("- Wins: ${summaries.numberOfWinsPlayerUnderTest}")
            
            val winPercentage = (summaries.numberOfWinsPlayerUnderTest.toDouble() / summaries.numberOfGames) * 100.0

            result.add("- Win Percentage: ${String.format("%.1f", winPercentage)}%")
            
            // Report first player advantage statistics
            result.add("First Player Advantage Statistics:")
            result.add("- First Player Wins: ${summaries.playerWhoWonWasFirst}")
            
            val firstPlayerWinPercentage = (summaries.playerWhoWonWasFirst.toDouble() / summaries.numberOfGames) * 100.0
            result.add("- First Player Win Percentage: ${String.format("%.1f", firstPlayerWinPercentage)}%")
            
            // Battle phase statistics
            result.add("")
            result.add("Battle Phase Statistics:")
            result.add(formatSummary("Battle Transition", summaries.battleTransition))
            result.add(formatSummary("Narrowest Battle Gap", summaries.narrowestBattleGap))
            result.add(formatSummary("Widest Battle Gap", summaries.widestBattleGap))
            
            // Game length statistics
            result.add("")
            result.add("Game Length Statistics:")
            result.add(formatSummary("Number of Turns", summaries.numberOfTurns))
            
            // Lead change statistics
            result.add("")
            result.add("Lead Change Statistics:")
            result.add(formatSummary("Flips", summaries.numberOfFlips))
            result.add(formatSummary("Cultivation Flips", summaries.numberOfCultivationFlips))
            result.add(formatSummary("Battle Flips", summaries.numberOfBattleFlips))
            
            // Score statistics
            result.add("")
            result.add("Score Statistics:")
            result.add(formatSummary("Max Score", summaries.maxScore))
            result.add(formatSummary("Average Gap Change", summaries.averageGapChange))
            result.add(formatSummary("Largest Gap Change", summaries.largestGapChange))
        }
        
        return result
    }
    
    /**
     * Format a Summary object into a readable string
     */
    private fun formatSummary(label: String, summary: Summary): String {
        return "- $label: min=${summary.smallest}, max=${summary.largest}, avg=${summary.average}, stv=${summary.standardDeviation}"
    }
}