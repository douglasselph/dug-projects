package dugsolutions.leaf.chronicle.domain

data class GameSummary(
    val totalTurns: Int,             // Total turns the game took
    val playerIdUnderTest: Int,      // The ID of the player under test.
    val placeDistribution: List<Int>,// Player IDs in the order of their final places (index 0 = 1st place)
    val playerIdOfFirstPlayer : Int, // The player id of the player who had the first move.
    val battleTransitionOnTurn: Int, // Turn when game transitioned to Battle Phase
    val narrowestBattleGap: Int,     // The smallest score gap on a turn between players
    val widestBattleGap: Int,        // The widest score gape on a turn between players
    val numberOfFlips: Int,          // The number of flips during the game
    val numberOfCultivationFlips: Int, // The number of flips during the game
    val numberOfBattleFlips: Int,    // The number of flips during the game
    val maxScore: Int,               // The maximum score reached
    val averageGapChange: Int,       // The average amount the gap changed from turn to turn.
    val largestGapChange: Int        // The largest amount the gap changed on a turn.
)