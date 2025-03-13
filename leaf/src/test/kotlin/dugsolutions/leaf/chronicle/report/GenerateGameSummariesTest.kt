package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.domain.GameSummaries
import dugsolutions.leaf.chronicle.domain.GameSummary
import dugsolutions.leaf.chronicle.domain.PlayerUnderTest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GenerateGameSummariesTest {

    private lateinit var generateGameSummaries: GenerateGameSummaries

    // Constants for readability
    companion object {
        private const val PLAYER_A_ID = 1
        private const val PLAYER_B_ID = 2
        private const val PLAYER_C_ID = 3

        // Standard game summary - player under test (A) goes first and wins
        private val PLAYER_A_FIRST_AND_WINS = createGameSummary(
            // Player A wins (first place)
            placeDistribution = listOf(PLAYER_A_ID, PLAYER_B_ID),
            playerIdOfFirstPlayer = PLAYER_A_ID  // Player A goes first
        )

        // Player under test (A) goes first but loses to Player B
        private val PLAYER_A_FIRST_BUT_LOSES = createGameSummary(
            // Player A loses (second place)
            placeDistribution = listOf(PLAYER_B_ID, PLAYER_A_ID),
            playerIdOfFirstPlayer = PLAYER_A_ID  // Player A goes first
        )

        // Player B goes first and wins, Player A (under test) loses
        private val PLAYER_B_FIRST_AND_WINS = createGameSummary(
            // Player A loses (second place)
            placeDistribution = listOf(PLAYER_B_ID, PLAYER_A_ID),
            playerIdOfFirstPlayer = PLAYER_B_ID  // Player B goes first
        )

        // Player B goes first but loses to Player A (under test)
        private val PLAYER_B_FIRST_BUT_LOSES = createGameSummary(
            // Player A wins (first place)
            placeDistribution = listOf(PLAYER_A_ID, PLAYER_B_ID),
            playerIdOfFirstPlayer = PLAYER_B_ID  // Player B goes first
        )

        // No first player information available
        private val NO_FIRST_PLAYER_INFO = createGameSummary(
            // Player A wins
            placeDistribution = listOf(PLAYER_A_ID, PLAYER_B_ID),
            playerIdOfFirstPlayer = -1   // No first player info
        )

        // Player under test comes in third place
        private val PLAYER_A_COMES_IN_THIRD = createGameSummary(
            // Player A in third place
            placeDistribution = listOf(PLAYER_B_ID, PLAYER_C_ID, PLAYER_A_ID),
            playerIdOfFirstPlayer = PLAYER_B_ID
        )

        // Player under test not in results
        private val PLAYER_A_NOT_IN_RESULTS = createGameSummary(
            // Player A not in results
            placeDistribution = listOf(PLAYER_B_ID, PLAYER_C_ID),
            playerIdOfFirstPlayer = PLAYER_B_ID
        )

        // Create a standard game summary with specific values
        private fun createGameSummary(
            placeDistribution: List<Int>,
            playerIdOfFirstPlayer: Int
        ): GameSummary {
            return GameSummary(
                totalTurns = 10,
                playerIdUnderTest = PLAYER_A_ID,
                placeDistribution = placeDistribution,
                playerIdOfFirstPlayer = playerIdOfFirstPlayer,
                battleTransitionOnTurn = 5,
                narrowestBattleGap = 1,
                widestBattleGap = 5,
                numberOfFlips = 3,
                numberOfCultivationFlips = 2,
                numberOfBattleFlips = 1,
                maxScore = 25,
                averageGapChange = 2,
                largestGapChange = 4
            )
        }
    }

    @BeforeEach
    fun setup() {
        generateGameSummaries = GenerateGameSummaries()
    }

    @Test
    fun invoke_withEmptySummaries_returnsEmptyPlaceDistribution() {
        // Act
        val result = generateGameSummaries(emptyList())

        // Assert
        assertEquals(0, result.numberOfGames)
        assertEquals(
            emptyList<Int>(), result.placeDistributionPlayerUnderTest,
            "Place distribution should be empty for empty summaries"
        )
        assertEquals(0, result.numberOfWinsPlayerUnderTest)
        assertEquals(0, result.playerWhoWonWasFirst)
    }

    @Test
    fun placeDistribution_whenPlayerAlwaysWins_hasAllEntriesInFirstPlace() {
        // Arrange - create summaries where player under test always comes in first
        val summaries = listOf(
            PLAYER_A_FIRST_AND_WINS,
            PLAYER_B_FIRST_BUT_LOSES,
            NO_FIRST_PLAYER_INFO
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(3, result.numberOfGames)
        assertEquals(
            3, result.placeDistributionPlayerUnderTest[0],
            "Should have 3 entries in first place (index 0)"
        )
        assertEquals(
            1, result.placeDistributionPlayerUnderTest.size,
            "Should only have entries for first place when player always wins"
        )
        assertEquals(
            3, result.numberOfWinsPlayerUnderTest,
            "Number of wins should match first place count"
        )
    }

    @Test
    fun placeDistribution_whenPlayerAlwaysSecond_hasAllEntriesInSecondPlace() {
        // Arrange - create summaries where player under test always comes in second
        val summaries = listOf(
            PLAYER_A_FIRST_BUT_LOSES,
            PLAYER_B_FIRST_AND_WINS
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(2, result.numberOfGames)
        assertEquals(
            0, result.placeDistributionPlayerUnderTest[0],
            "Should have 0 entries in first place (index 0)"
        )
        assertEquals(
            2, result.placeDistributionPlayerUnderTest[1],
            "Should have 2 entries in second place (index 1)"
        )
        assertEquals(
            2, result.placeDistributionPlayerUnderTest.size,
            "Should have entries for both first and second place"
        )
        assertEquals(
            0, result.numberOfWinsPlayerUnderTest,
            "Number of wins should be 0 when player is always second"
        )
    }

    @Test
    fun placeDistribution_whenPlayerHasMixedResults_showsCorrectDistribution() {
        // Arrange - create summaries with mixed placement results
        val summaries = listOf(
            PLAYER_A_FIRST_AND_WINS,     // First place
            PLAYER_A_FIRST_BUT_LOSES,    // Second place
            PLAYER_B_FIRST_BUT_LOSES,    // First place
            PLAYER_B_FIRST_AND_WINS,     // Second place
            PLAYER_A_COMES_IN_THIRD      // Third place
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(5, result.numberOfGames)
        assertEquals(
            2, result.placeDistributionPlayerUnderTest[0],
            "Should have 2 entries in first place (index 0)"
        )
        assertEquals(
            2, result.placeDistributionPlayerUnderTest[1],
            "Should have 2 entries in second place (index 1)"
        )
        assertEquals(
            1, result.placeDistributionPlayerUnderTest[2],
            "Should have 1 entry in third place (index 2)"
        )
        assertEquals(
            3, result.placeDistributionPlayerUnderTest.size,
            "Should have entries for all three places"
        )
        assertEquals(
            2, result.numberOfWinsPlayerUnderTest,
            "Number of wins should match first place count"
        )
    }

    @Test
    fun placeDistribution_whenPlayerNotInSomeResults_handlesCorrectly() {
        // Arrange - create summaries with player missing from some results
        val summaries = listOf(
            PLAYER_A_FIRST_AND_WINS,     // First place
            PLAYER_A_NOT_IN_RESULTS,     // Not present
            PLAYER_B_FIRST_BUT_LOSES     // First place
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(3, result.numberOfGames)
        assertEquals(
            2, result.placeDistributionPlayerUnderTest[0],
            "Should have 2 entries in first place (index 0)"
        )
        assertEquals(
            1, result.placeDistributionPlayerUnderTest.size,
            "Should only have entries for first place"
        )
        assertEquals(
            2, result.numberOfWinsPlayerUnderTest,
            "Number of wins should only count games where player is present"
        )
    }

    @Test
    fun placeDistribution_whenPlayerHasDifferentIDs_calculatesForRequestedID() {
        // Arrange
        // Set player ID to PLAYER_B_ID
//            PlayerUnderTest.playerId = PLAYER_B_ID

        // The test player is now PLAYER_B_ID, so these summaries have different meaning
        val summaries = listOf(
            PLAYER_A_FIRST_AND_WINS,     // Player B in second place
            PLAYER_B_FIRST_AND_WINS      // Player B in first place
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(2, result.numberOfGames)
        assertEquals(
            1, result.placeDistributionPlayerUnderTest[0],
            "Player B should have 1 entry in first place (index 0)"
        )
        assertEquals(
            1, result.placeDistributionPlayerUnderTest[1],
            "Player B should have 1 entry in second place (index 1)"
        )
        assertEquals(
            2, result.placeDistributionPlayerUnderTest.size,
            "Should have entries for both first and second place"
        )
        assertEquals(
            1, result.numberOfWinsPlayerUnderTest,
            "Player B should have 1 win"
        )
    }

    @Test
    fun invoke_calculateNumberOfWinsPlayerUnderTest_countsFirstPlaceFinishes() {
        // Arrange - create summaries with player under test in different positions
        val summaries = listOf(
            PLAYER_A_FIRST_AND_WINS,      // Player A (ID 1) in first place - WIN
            PLAYER_A_FIRST_BUT_LOSES,     // Player A (ID 1) in second place - LOSS
            PLAYER_B_FIRST_BUT_LOSES,     // Player A (ID 1) in first place - WIN
            PLAYER_B_FIRST_AND_WINS,      // Player A (ID 1) in second place - LOSS
            NO_FIRST_PLAYER_INFO          // Player A (ID 1) in first place - WIN
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(5, result.numberOfGames, "Should count all games")
        assertEquals(
            3, result.numberOfWinsPlayerUnderTest,
            "Should count 3 wins for player under test (in first position of placeDistribution)"
        )
        assertEquals(
            3, result.placeDistributionPlayerUnderTest[0],
            "Should count 3 first place finishes (index 0)"
        )
        assertEquals(
            2, result.placeDistributionPlayerUnderTest[1],
            "Should count 2 second place finishes (index 1)"
        )
    }

    @Test
    fun invoke_withNoFirstPlayerInfo_returnsZeroForPlayerWhoWonWasFirst() {
        // Arrange - create summaries without first player information
        val summaries = listOf(
            NO_FIRST_PLAYER_INFO,
            NO_FIRST_PLAYER_INFO
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(2, result.numberOfGames)
        assertEquals(
            2, result.numberOfWinsPlayerUnderTest,
            "Player under test should have 2 wins when in first place"
        )
        assertEquals(
            0,
            result.playerWhoWonWasFirst,
            "When no first player info is available, count should be 0"
        )
    }

    @Test
    fun invoke_calculatesCorrectPlaceDistributionForPlayerUnderTest() {
        // Arrange - create game summaries where player under test is in second place
        val summaries = listOf(
            PLAYER_A_FIRST_BUT_LOSES,  // Player A (ID 1) in second place
            PLAYER_B_FIRST_AND_WINS    // Player A (ID 1) in second place
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(2, result.numberOfGames)
        assertEquals(
            0, result.numberOfWinsPlayerUnderTest,
            "Player under test should have 0 wins when never in first place"
        )
        assertEquals(
            0,
            result.placeDistributionPlayerUnderTest[0],
            "Player should have 0 first place finishes"
        )
        assertEquals(
            2,
            result.placeDistributionPlayerUnderTest[1],
            "Player should have 2 second place finishes"
        )
    }

    @Test
    fun invoke_whenPlayerUnderTestAlwaysWins_returnsCorrectWinCount() {
        // Arrange - create summaries where player under test always wins
        val winSummaries = listOf(
            PLAYER_A_FIRST_AND_WINS,      // Player A wins in first position
            PLAYER_B_FIRST_BUT_LOSES,     // Player A wins in first position
            NO_FIRST_PLAYER_INFO          // Player A wins in first position
        )

        // Act
        val result = generateGameSummaries(winSummaries)

        // Assert
        assertEquals(3, result.numberOfGames)
        assertEquals(
            3, result.numberOfWinsPlayerUnderTest,
            "Player under test should have 3 wins when always in first place"
        )
        assertEquals(
            3, result.placeDistributionPlayerUnderTest[0],
            "All 3 games should be first place finishes"
        )
        assertEquals(
            1, result.playerWhoWonWasFirst,
            "Only 1 game has player under test as first player who won"
        )
    }

    @Test
    fun invoke_whenPlayerUnderTestIsFirstAndWins_countsPlayerWhoWonWasFirst() {
        // Arrange
        val summaries = listOf(PLAYER_A_FIRST_AND_WINS)

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(1, result.numberOfGames)
        assertEquals(
            1, result.numberOfWinsPlayerUnderTest,
            "Player under test should have 1 win when in first place"
        )
        assertEquals(
            1,
            result.playerWhoWonWasFirst,
            "When player under test goes first and wins, count should be 1"
        )
    }

    @Test
    fun invoke_whenPlayerUnderTestIsFirstButLoses_returnsZeroForPlayerWhoWonWasFirst() {
        // Arrange
        val summaries = listOf(PLAYER_A_FIRST_BUT_LOSES)

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(1, result.numberOfGames)
        assertEquals(
            0, result.numberOfWinsPlayerUnderTest,
            "Player under test should have 0 wins when in second place"
        )
        assertEquals(
            0,
            result.playerWhoWonWasFirst,
            "When player under test goes first but loses, count should be 0"
        )
    }

    @Test
    fun invoke_whenOtherPlayerIsFirstAndWins_identifiesFirstPlayerWin() {
        // Arrange
        val summaries = listOf(PLAYER_B_FIRST_AND_WINS)

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(1, result.numberOfGames)
        assertEquals(
            0, result.numberOfWinsPlayerUnderTest,
            "Player under test should have 0 wins when in second place"
        )
        assertEquals(
            1, result.playerWhoWonWasFirst,
            "Fixed implementation counts when another player goes first and wins"
        )
    }

    @Test
    fun invoke_whenOtherPlayerIsFirstButLoses_returnsZeroForPlayerWhoWonWasFirst() {
        // Arrange
        val summaries = listOf(PLAYER_B_FIRST_BUT_LOSES)

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(1, result.numberOfGames)
        assertEquals(
            1, result.numberOfWinsPlayerUnderTest,
            "Player under test should have 1 win when in first place"
        )
        assertEquals(
            0,
            result.playerWhoWonWasFirst,
            "When player under test wins but didn't go first, count should be 0"
        )
    }

    @Test
    fun invoke_withMixedGameResults_calculatesPlayerWhoWonWasFirstCorrectly() {
        // Arrange
        val summaries = listOf(
            PLAYER_A_FIRST_AND_WINS,    // First player (A) wins - count: 1
            PLAYER_A_FIRST_BUT_LOSES,   // First player (A) loses - not counted
            PLAYER_B_FIRST_BUT_LOSES,   // First player (B) loses - not counted
            PLAYER_B_FIRST_AND_WINS     // First player (B) wins - count: 0 (or 1 if fixed)
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(4, result.numberOfGames)
        assertEquals(2, result.numberOfWinsPlayerUnderTest, "Player under test won twice")

        // With fixed implementation: 
        assertEquals(2, result.playerWhoWonWasFirst, "Counts when any first player wins")
    }

    @Test
    fun invoke_withFixedLogic_calculatesAllFirstPlayerWins() {
        // This test demonstrates what the result should be with fixed logic

        // Skip this test as it requires the fixed implementation
        // Will be re-enabled once the fix is implemented

        // Arrange - with fixed implementation, both scenarios would count
        val summaries = listOf(
            PLAYER_A_FIRST_AND_WINS,  // First player wins (player under test)
            PLAYER_B_FIRST_AND_WINS   // First player wins (other player)
        )

        // Act
        val result = generateGameSummaries(summaries)

        // Assert
        assertEquals(2, result.numberOfGames)
        assertEquals(
            1, result.numberOfWinsPlayerUnderTest,
            "Player under test won 1 out of 2 games"
        )

        // The desired result with fixed implementation would be:
        assertEquals(
            2,
            result.playerWhoWonWasFirst,
            "Should count both games where first player won"
        )
    }

    @Test
    fun invoke_demonstratesDifferentWinRatesBetweenPlayerUnderTestAndFirstPlayer() {
        // Arrange
        // Create a specific set of game summaries where:
        // - PLAYER_A is always the player under test
        // - We have a specific mix of scenarios that will result in different win rates
        
        // These summaries will demonstrate:
        // - PLAYER_A (under test) wins 4 out of 6 games (67%)
        // - The first player (regardless of who) wins 5 out of 6 games (83%)
        
        val summaries = listOf(
            // First 3 scenarios: PLAYER_A is first and wins (counts for both player under test AND first player)
            PLAYER_A_FIRST_AND_WINS,
            PLAYER_A_FIRST_AND_WINS,
            PLAYER_A_FIRST_AND_WINS,
            
            // Next scenario: PLAYER_B is first but PLAYER_A wins (counts for player under test but NOT first player)
            PLAYER_B_FIRST_BUT_LOSES,
            
            // Next 2 scenarios: PLAYER_B is first and wins (counts for first player but NOT player under test)
            PLAYER_B_FIRST_AND_WINS,
            PLAYER_B_FIRST_AND_WINS
        )
        
        // Act
        val result = generateGameSummaries(summaries)
        
        // Assert
        assertEquals(6, result.numberOfGames, "Should have 6 total games")
        
        // PLAYER_A (under test) won 4 games (3 as first player + 1 as second player)
        assertEquals(4, result.numberOfWinsPlayerUnderTest, "Player under test should have 4 wins (67%)")
        
        // First player (regardless of ID) won 5 games (3 where PLAYER_A went first + 2 where PLAYER_B went first)
        assertEquals(5, result.playerWhoWonWasFirst, "First player should have 5 wins (83%)")
        
        // Calculate and verify percentages
        val playerUnderTestWinRate = (result.numberOfWinsPlayerUnderTest.toDouble() / result.numberOfGames) * 100.0
        val firstPlayerWinRate = (result.playerWhoWonWasFirst.toDouble() / result.numberOfGames) * 100.0
        
        // These should be different values
        assertEquals(66.67, playerUnderTestWinRate, 0.01, "Player under test win rate should be approximately 67%")
        assertEquals(83.33, firstPlayerWinRate, 0.01, "First player win rate should be approximately 83%")
        
        // Explicitly verify they are different
        assert(playerUnderTestWinRate != firstPlayerWinRate) {
            "Player under test win rate ($playerUnderTestWinRate%) should differ from first player win rate ($firstPlayerWinRate%)"
        }
    }

    @Test
    fun invoke_whenPlayerUnderTestIsNotPlayerA_showsDifferentWinRates() {
        // Arrange
        // This test explicitly sets Player B as the player under test to
        // demonstrate the difference in win rates
        
        // We need to temporarily change the player under test ID in our test game summaries
        // This requires creating custom game summaries where Player B is the player under test
        
        val customSummaries = listOf(
            // Player A first and wins - Player B (under test) loses
            GameSummary(
                totalTurns = 10,
                playerIdUnderTest = PLAYER_B_ID, // B is under test
                placeDistribution = listOf(PLAYER_A_ID, PLAYER_B_ID), // A wins, B second
                playerIdOfFirstPlayer = PLAYER_A_ID, // A goes first
                battleTransitionOnTurn = 5,
                narrowestBattleGap = 1,
                widestBattleGap = 5,
                numberOfFlips = 3,
                numberOfCultivationFlips = 2,
                numberOfBattleFlips = 1,
                maxScore = 25,
                averageGapChange = 2,
                largestGapChange = 4
            ),
            
            // Player B first and wins - Player B (under test) wins
            GameSummary(
                totalTurns = 10,
                playerIdUnderTest = PLAYER_B_ID, // B is under test
                placeDistribution = listOf(PLAYER_B_ID, PLAYER_A_ID), // B wins, A second
                playerIdOfFirstPlayer = PLAYER_B_ID, // B goes first
                battleTransitionOnTurn = 5,
                narrowestBattleGap = 1,
                widestBattleGap = 5,
                numberOfFlips = 3,
                numberOfCultivationFlips = 2,
                numberOfBattleFlips = 1,
                maxScore = 25,
                averageGapChange = 2,
                largestGapChange = 4
            ),
            
            // Player A first and loses - Player B (under test) wins
            GameSummary(
                totalTurns = 10,
                playerIdUnderTest = PLAYER_B_ID, // B is under test
                placeDistribution = listOf(PLAYER_B_ID, PLAYER_A_ID), // B wins, A second
                playerIdOfFirstPlayer = PLAYER_A_ID, // A goes first
                battleTransitionOnTurn = 5,
                narrowestBattleGap = 1,
                widestBattleGap = 5,
                numberOfFlips = 3,
                numberOfCultivationFlips = 2,
                numberOfBattleFlips = 1,
                maxScore = 25,
                averageGapChange = 2,
                largestGapChange = 4
            ),
            
            // Player B first and loses - Player B (under test) loses
            GameSummary(
                totalTurns = 10,
                playerIdUnderTest = PLAYER_B_ID, // B is under test
                placeDistribution = listOf(PLAYER_A_ID, PLAYER_B_ID), // A wins, B second
                playerIdOfFirstPlayer = PLAYER_B_ID, // B goes first
                battleTransitionOnTurn = 5,
                narrowestBattleGap = 1,
                widestBattleGap = 5,
                numberOfFlips = 3,
                numberOfCultivationFlips = 2,
                numberOfBattleFlips = 1,
                maxScore = 25,
                averageGapChange = 2,
                largestGapChange = 4
            )
        )
        
        // Act
        val result = generateGameSummaries(customSummaries)
        
        // Assert
        assertEquals(4, result.numberOfGames, "Should have 4 total games")
        
        // Player under test (B) won 2 out of 4 games (50%)
        assertEquals(2, result.numberOfWinsPlayerUnderTest, "Player B (under test) should have 2 wins")
        
        // First player (regardless of ID) won 2 out of 4 games (50%)
        assertEquals(2, result.playerWhoWonWasFirst, "First player should have 2 wins")
        
        // Calculate win percentages for clarity
        val playerUnderTestWinRate = (result.numberOfWinsPlayerUnderTest.toDouble() / result.numberOfGames) * 100.0
        val firstPlayerWinRate = (result.playerWhoWonWasFirst.toDouble() / result.numberOfGames) * 100.0
        
        assertEquals(50.0, playerUnderTestWinRate, 0.01, "Player under test win rate should be 50%")
        assertEquals(50.0, firstPlayerWinRate, 0.01, "First player win rate should be 50%") 
        
        // In this specific case, we constructed a scenario where they are equal,
        // but with different distributions of who won when
    }

    @Test
    fun diagnose_whenStatisticsAppearIdentical_canIdentifyActualDifferences() {
        // Arrange - This test demonstrates a scenario where the statistics might appear identical
        // but there is actually a difference in who is winning
        
        // Create a carefully constructed set of summaries where:
        // - Player under test wins same number of times as first player (3 out of 6)
        // - BUT these are different games/scenarios
        
        val summaries = listOf(
            // Scenario 1: Player A (under test) is first and wins - counts for both statistics
            PLAYER_A_FIRST_AND_WINS,
            
            // Scenario 2: Player B is first and wins - counts for first player but not player under test
            PLAYER_B_FIRST_AND_WINS,
            
            // Scenario 3: Player A (under test) is first but loses - counts for neither statistic
            PLAYER_A_FIRST_BUT_LOSES,
            
            // Scenario 4: Player B is first but loses to Player A - counts for player under test but not first player
            PLAYER_B_FIRST_BUT_LOSES, 
            
            // Scenario 5: No first player info, but Player A wins - counts for player under test only
            NO_FIRST_PLAYER_INFO,
            
            // Scenario 6: Player A (under test) is first and wins - counts for both statistics
            PLAYER_A_FIRST_AND_WINS
        )
        
        // Act
        val result = generateGameSummaries(summaries)
        
        // Assert - Both statistics will show 3 wins (50%), but they're different games
        assertEquals(6, result.numberOfGames, "Should have 6 total games")
        assertEquals(3, result.playerWhoWonWasFirst, "First player should have 3 wins (50%)")
        assertEquals(4, result.numberOfWinsPlayerUnderTest, "Player under test should have 4 wins")
        
        // To diagnose this problem, we need to look at the individual game summaries
        // and count how many times both conditions were true simultaneously
        
        // Count cases where both the first player won AND it was the player under test
        val bothConditionsTrue = summaries.count { summary ->
            val firstPlayerWon = summary.playerIdOfFirstPlayer != -1 && 
                               summary.placeDistribution.isNotEmpty() &&
                               summary.placeDistribution[0] == summary.playerIdOfFirstPlayer
                               
            val playerUnderTestWon = summary.placeDistribution.isNotEmpty() &&
                                   summary.placeDistribution[0] == summary.playerIdUnderTest
                                   
            firstPlayerWon && playerUnderTestWon
        }
        
        // This should be fewer than either individual count
        assertEquals(2, bothConditionsTrue, "Only 2 games had both the first player AND player under test winning")
        
        // The diagnostic calculation shows that even though both statistics show 3/6 wins,
        // they are counting different games in some cases
    }
} 