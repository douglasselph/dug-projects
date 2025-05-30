package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.EventBattle
import dugsolutions.leaf.chronicle.domain.EventTurn
import dugsolutions.leaf.chronicle.domain.GameSummary
import dugsolutions.leaf.chronicle.domain.OrderingEntry
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.chronicle.domain.PlayerUnderTest
import dugsolutions.leaf.chronicle.domain.ScoreInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GenerateGameSummaryTest {

    // Create real test implementations of the classes
    companion object {

        private const val PLAYER_A_ID = 1
        private const val PLAYER_B_ID = 2
        private const val TURN = 10

        // Create a ScorePlayer implementation for testing
        
        // Create a ScoreInfo implementation for testing
        private fun createScoreInfo(playerId: Int, scoreDice: Int): ScoreInfo {
            return ScoreInfo(PlayerScore(playerId = playerId, scoreDice = scoreDice))
        }
        
        // Create an EventTurn implementation for testing
        private fun createEventTurn(turn: Int, scores: List<ScoreInfo>): EventTurn {
            return EventTurn(
                turn = turn,
                reports = emptyList(), // Empty reports for test simplicity
                scores = scores
            )
        }
        
        // Create an EventBattle implementation for testing
        private fun createEventBattle(turn: Int, scores: List<ScoreInfo>): EventBattle {
            return EventBattle(
                turn = turn,
                scores = scores
            )
        }
        
        // Create an OrderingEntry implementation for testing
        private fun createOrderingEntry(turn: Int, playerIdOrder: List<Int>): OrderingEntry {
            return OrderingEntry(
                turn = turn,
                playerOrder = playerIdOrder,
                reports = emptyList()
            )
        }
        
        // Create a game summary where the first player won
        private fun createGameSummaryWithFirstPlayerWin(): GameSummary {
            // Player under test (ID 1) in second place, Player 2 in first place
            return GameSummary(
                totalTurns = TURN,
                playerIdUnderTest = PLAYER_A_ID,
                placeDistribution = listOf(2, 1, 0), // Player 2 first, Player 1 (under test) second
                playerIdOfFirstPlayer = 2,  // Player 2 went first
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
        
        // Create a game summary where the first player lost
        private fun createGameSummaryWithFirstPlayerLoss(): GameSummary {
            // Player under test (ID 1) in first place, Player 1 went first but lost
            return GameSummary(
                totalTurns = TURN,
                playerIdUnderTest = PLAYER_A_ID,
                placeDistribution = listOf(1, 0, 2), // Player 1 (under test) first, Player 0 second
                playerIdOfFirstPlayer = 0,  // Player 0 went first (not the player under test)
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

    private lateinit var mockChronicle: GameChronicle
    private lateinit var playerUnderTest: PlayerUnderTest
    private lateinit var generateGameSummary: GenerateGameSummary

    @BeforeEach
    fun setup() {
        playerUnderTest = mockk(relaxed = true)
        mockChronicle = mockk(relaxed = true)
        every { playerUnderTest.playerId } returns PLAYER_A_ID

        generateGameSummary = GenerateGameSummary(mockChronicle, playerUnderTest)

    }

    @Test
    fun invoke_whenPlayerUnderTestWins_returnsZeroPlaceForPlayerUnderTest() {
        // Arrange
        // Create battle entry
        val battleEntry = createEventBattle(
            turn = 5,
            scores = listOf(
                createScoreInfo(playerId = PLAYER_A_ID, scoreDice = 15),
                createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 12)
            )
        )
        
        // Create turn entries where player under test has higher score
        val turnEntries = listOf(
            // First turn - player under test starts ahead
            createEventTurn(
                turn = 1,
                scores = listOf(
                    createScoreInfo(playerId = PLAYER_A_ID, scoreDice = 5),
                    createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 3)
                )
            ),
            // Last turn - player under test wins
            createEventTurn(
                turn = TURN,
                scores = listOf(
                    createScoreInfo(playerId = PLAYER_A_ID, scoreDice = 25),
                    createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 20)
                )
            )
        )
        
        val chronicleEntries = turnEntries + battleEntry
        every { mockChronicle.getEntries() } returns chronicleEntries
        
        // Act
        val result = generateGameSummary()
        
        // Assert
        assertEquals(0, result.placeDistribution.indexOf(PLAYER_A_ID), "Player under test should be in first place (index 0)")
    }
    
    @Test
    fun invoke_whenPlayerUnderTestLoses_returnsFirstPlaceForPlayerUnderTest() {
        // Arrange
        // Create battle entry
        val battleEntry = createEventBattle(
            turn = 5,
            scores = listOf(
                createScoreInfo(playerId = PLAYER_A_ID, scoreDice = 12),
                createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 15)
            )
        )
        
        // Create turn entries where player 1 has higher score
        val turnEntries = listOf(
            // First turn - player 1 starts ahead
            createEventTurn(
                turn = 1,
                scores = listOf(
                    createScoreInfo(playerId = PLAYER_A_ID, scoreDice = 3),
                    createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 5)
                )
            ),
            // Last turn - player 1 wins
            createEventTurn(
                turn = TURN,
                scores = listOf(
                    createScoreInfo(playerId = PLAYER_A_ID, scoreDice = 18),
                    createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 22)
                )
            )
        )
        
        val chronicleEntries = turnEntries + battleEntry
        every { mockChronicle.getEntries() } returns chronicleEntries
        
        // Act
        val result = generateGameSummary()
        
        // Assert
        assertEquals(1, result.placeDistribution.indexOf(PLAYER_A_ID), "Player under test should be in second place (index 1)")
    }
    
    @Test
    fun invoke_withNoEntries_returnsDefaultValues() {
        // Arrange
        every { mockChronicle.getEntries() } returns emptyList()
        
        // Act
        val result = generateGameSummary()
        
        // Assert
        assertEquals(emptyList<Int>(), result.placeDistribution, "Place distribution should be empty")
        assertEquals(0, result.totalTurns, "Total turns should be 0")
        assertEquals(0, result.battleTransitionOnTurn, "Battle transition turn should be 0")
        assertEquals(-1, result.playerIdOfFirstPlayer, "First player ID should be -1 when there are no entries")
    }
    
    @Test
    fun invoke_whenScoresDontHavePlayerUnderTestId_returnsInvalidPlace() {
        // Arrange
        // Create turn entries where player under test isn't present
        val turnEntries = listOf(
            createEventTurn(
                turn = TURN,
                scores = listOf(
                    createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 22),
                    createScoreInfo(playerId = 2, scoreDice = 18)
                )
            )
        )
        
        every { mockChronicle.getEntries() } returns turnEntries
        
        // Act
        val result = generateGameSummary()
        
        // Assert
        assertEquals(-1, result.placeDistribution.indexOf(0), "Player under test should not be found in place distribution")
    }
    
    @Test
    fun invoke_withOrderingEntry_setsFirstPlayerId() {
        // Arrange
        val turn = TURN
        // Create ordering entry where player 1 goes first
        val orderingEntry = createOrderingEntry(
            turn = turn,
            playerIdOrder = listOf(PLAYER_A_ID, PLAYER_B_ID)
        )
        
        // Create turn entries
        val turnEntries = listOf(
            createEventTurn(
                turn = turn,
                scores = listOf(
                    createScoreInfo(playerId = PLAYER_A_ID, scoreDice = 18),
                    createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 22),
                    createScoreInfo(playerId = 2, scoreDice = 15)
                )
            )
        )
        
        val chronicleEntries = turnEntries + orderingEntry
        every { mockChronicle.getEntries() } returns chronicleEntries
        
        // Act
        val result = generateGameSummary()
        
        // Assert
        assertEquals(PLAYER_A_ID, result.playerIdOfFirstPlayer, "Player ID $PLAYER_A_ID should be identified as the first player")
        assertEquals(1, result.placeDistribution.indexOf(PLAYER_A_ID), "Player under test (ID $PLAYER_A_ID) should be in second place (index 1)")
    }
    
    @Test
    fun invoke_whenFirstPlayerWins_correctlyIdentifiesFirstPlayerWin() {
        // Arrange
        // Set player under test ID before test
        every { playerUnderTest.playerId } returns PLAYER_B_ID

        // Create ordering entry where player under test (ID 1) goes first
        val orderingEntry = createOrderingEntry(
            turn = TURN,
            playerIdOrder = listOf(PLAYER_B_ID, PLAYER_A_ID)
        )
        
        // Create turn entries where player 1 (who went first) wins
        val turnEntries = listOf(
            createEventTurn(
                turn = TURN,
                scores = listOf(
                    createScoreInfo(playerId = PLAYER_A_ID, scoreDice = 18),
                    createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 22), // Player 1 (who went first) wins
                    createScoreInfo(playerId = 2, scoreDice = 15)
                )
            )
        )
        
        val chronicleEntries = turnEntries + orderingEntry
        every { mockChronicle.getEntries() } returns chronicleEntries
        
        // Act
        val result = generateGameSummary()
        
        // Assert
        assertEquals(PLAYER_B_ID, result.playerIdOfFirstPlayer, "Player ID $PLAYER_B_ID should be identified as the first player")
        assertEquals(0, result.placeDistribution.indexOf(PLAYER_B_ID), "Player under test (ID $PLAYER_B_ID) should be in first place (index 0)")
        // In a real game, this would enable calculating first player advantage
    }
    
    @Test
    fun invoke_whenFirstPlayerLoses_correctlyIdentifiesFirstPlayerDidntWin() {
        // Arrange
        // Set player under test ID before test
        every { playerUnderTest.playerId } returns PLAYER_B_ID

        // Create ordering entry where player 0 goes first
        val orderingEntry = createOrderingEntry(
            turn = TURN,
            playerIdOrder = listOf(PLAYER_A_ID, PLAYER_B_ID)
        )
        
        // Create turn entries where player 0 (who went first) loses to player 1
        val turnEntries = listOf(
            createEventTurn(
                turn = TURN,
                scores = listOf(
                    createScoreInfo(playerId = PLAYER_A_ID, scoreDice = 18), // Player 0 (who went first) loses
                    createScoreInfo(playerId = PLAYER_B_ID, scoreDice = 22), // Player 1 (player under test) wins
                    createScoreInfo(playerId = 2, scoreDice = 15)
                )
            )
        )
        
        val chronicleEntries = turnEntries + orderingEntry
        every { mockChronicle.getEntries() } returns chronicleEntries
        
        // Act
        val result = generateGameSummary()
        
        // Assert
        assertEquals(PLAYER_A_ID, result.playerIdOfFirstPlayer, "Player ID $PLAYER_A_ID should be identified as the first player")
        assertEquals(0, result.placeDistribution.indexOf(PLAYER_B_ID), "Player under test (ID 1) should be in first place (index 0)")
        // In a real game, this would enable calculating that first player advantage did not apply
    }
    
    @Test
    fun gameSummaries_calculatesPlayerWhoWonWasFirst() {
        // Arrange
        val generateGameSummaries = GenerateGameSummaries()
        
        // Set player under test ID
        every { playerUnderTest.playerId } returns PLAYER_B_ID

        // Create a list with two summaries - one where first player won, one where first player lost
        val summaries = listOf(
            createGameSummaryWithFirstPlayerWin(),
            createGameSummaryWithFirstPlayerLoss()
        )
        
        // Act
        val result = generateGameSummaries(summaries)
        
        // Assert
        assertEquals(2, result.numberOfGames, "Should have 2 games")
        assertEquals(1, result.numberOfWinsPlayerUnderTest, "Player under test should have 1 win")
        assertEquals(1, result.playerWhoWonWasFirst, "First player should have won once")
        
        // Verify the percentage can be calculated properly
        val firstPlayerWinPercentage = (result.playerWhoWonWasFirst.toDouble() / result.numberOfGames) * 100.0
        assertEquals(50.0, firstPlayerWinPercentage, "First player win percentage should be 50%")
    }
} 
