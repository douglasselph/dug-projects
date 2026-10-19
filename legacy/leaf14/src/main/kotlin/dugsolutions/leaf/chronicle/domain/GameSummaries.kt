package dugsolutions.leaf.chronicle.domain

data class Summary(
    val largest: Int,
    val smallest: Int,
    val average: Int,
    val standardDeviation: Int
)

data class GameSummaries(
    val numberOfGames: Int,               // Number of games.
    val numberOfWinsPlayerUnderTest: Int, // Number of wins the player under test had.
    val placeDistributionPlayerUnderTest: List<Int>, // What place the player under test had.
    val playerWhoWonWasFirst: Int,       // Number of times the player who was first also won.
    val numberOfTurns: Summary,          // Data on the number of turns.
    val battleTransition: Summary,       // Data when game transitioned to Battle Phase
    val narrowestBattleGap: Summary,     // Data on smallest score gap on a turn between players
    val widestBattleGap: Summary,        // Data on widest score gape on a turn between players
    val numberOfFlips: Summary,          // Data on number of flips during the game
    val numberOfCultivationFlips: Summary,
    val numberOfBattleFlips: Summary,
    val maxScore: Summary,               // Data on maximum score reached
    val averageGapChange: Summary,       // Data on amount the gap changed from turn to turn.
    val largestGapChange: Summary        // Data on amount the gap changed on a turn.
)
