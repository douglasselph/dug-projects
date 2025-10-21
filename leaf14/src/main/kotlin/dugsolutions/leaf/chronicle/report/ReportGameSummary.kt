package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.domain.GameSummary
import dugsolutions.leaf.chronicle.domain.PlayerUnderTest
import kotlin.math.roundToInt

class ReportGameSummary {

    operator fun invoke(summary: GameSummary): List<String> {
        val result = mutableListOf<String>()
        val playerIdUnderTest = summary.playerIdUnderTest
        val playerIdOfFirstPlayer = summary.playerIdOfFirstPlayer

        result.add("=== GAME SUMMARY ===")

        result.add("Total Turns: ${summary.totalTurns}")
        result.add("Player ID under test: $playerIdUnderTest")
        result.add("Player ID who went first: $playerIdOfFirstPlayer")
        // Report player under test results
        val placeName = placeOf(summary, playerIdUnderTest)
        result.add("Player Under Test Place: $placeName Place")
        val placeName2 = placeOf(summary, playerIdOfFirstPlayer)
        result.add("Player Who Was First Place: $placeName2 Place")

        // Report player placements
        if (summary.placeDistribution.isNotEmpty()) {
            result.add("Player Placements:")
            summary.placeDistribution.forEachIndexed { index, playerId ->
                val playerPlaceName = when (index) {
                    0 -> "First"
                    1 -> "Second"
                    2 -> "Third"
                    else -> "${index + 1}th"
                }
                result.add("  - $playerPlaceName Place: Player ID $playerId")
            }
        }

        // Report battle transition
        if (summary.battleTransitionOnTurn > 0) {
            val battlePercentage =
                ((summary.battleTransitionOnTurn.toDouble() / summary.totalTurns) * 100).roundToInt()
            result.add("Battle Phase Started: Turn ${summary.battleTransitionOnTurn} ($battlePercentage% of game)")

            // Battle-specific stats
            result.add("Battle Phase Stats:")
            result.add("  - Narrowest Gap: ${summary.narrowestBattleGap}")
            result.add("  - Widest Gap: ${summary.widestBattleGap}")
            result.add("  - Battle Flips: ${summary.numberOfBattleFlips}")
        } else {
            result.add("Battle Phase: Never Started")
        }

        // General game stats
        result.add("Game Stats:")
        result.add("  - Lead Changes: ${summary.numberOfFlips}")
        result.add("  - Cultivation Flips: ${summary.numberOfCultivationFlips}")
        result.add("  - Highest Score: ${summary.maxScore}")
        result.add("  - Avg Gap Change: ${summary.averageGapChange}")
        result.add("  - Largest Gap Change: ${summary.largestGapChange}")

        return result
    }

    private fun placeOf(summary: GameSummary, playerId: Int): String {
        return when (val playerPlace = summary.placeDistribution.indexOf(playerId)) {
            0 -> "First"
            1 -> "Second"
            2 -> "Third"
            -1 -> "Not Found"
            else -> "${playerPlace + 1}th"
        }
    }
}