package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.PlayersScoreData
import dugsolutions.leaf.player.domain.PlayerScoreData
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReportGameBriefTest {

    private lateinit var SUT: ReportGameBrief
    private lateinit var mockReportPlayer: ReportPlayer

    companion object {
        private const val PLAYER_A_ID = 1
        private const val PLAYER_B_ID = 2
        private const val TURN = 10
        private const val WINNER_SCORE = 25
        private const val LOSER_SCORE = 15
    }

    @BeforeEach
    fun setup() {
        mockReportPlayer = mockk()
        SUT = ReportGameBrief(mockReportPlayer)
    }

    @Test
    fun invoke_whenNoWinner_returnsEmptyList() {
        // Arrange
        val players = listOf<PlayerScoreData>()
        val result = PlayersScoreData(TURN, players)

        // Act
        val report = SUT(result)

        // Assert
        assertEquals(emptyList(), report)
    }

    @Test
    fun invoke_whenSinglePlayer_returnsCorrectReport() {
        // Arrange
        val score = PlayerScore(PLAYER_A_ID, WINNER_SCORE)
        val player = mockk<Player>()
        every { player.id } returns PLAYER_A_ID
        every { player.score } returns score

        val playerScoreData = PlayerScoreData(player, mockk())
        val result = PlayersScoreData(TURN, listOf(playerScoreData))

        every { mockReportPlayer(player) } returns "Player $PLAYER_A_ID: Score=[$WINNER_SCORE]"

        // Act
        val report = SUT(result)

        // Assert
        assertEquals(2, report.size)
        assertEquals("Winner $PLAYER_A_ID in $TURN, Score=[$WINNER_SCORE]", report[0])
        assertEquals("Player $PLAYER_A_ID: Score=[$WINNER_SCORE]", report[1])
    }

    @Test
    fun invoke_whenMultiplePlayers_returnsCorrectReport() {
        // Arrange
        val winner = mockk<Player>()
        val winnerScore = PlayerScore(PLAYER_A_ID, WINNER_SCORE)
        every { winner.id } returns PLAYER_A_ID
        every { winner.score } returns winnerScore

        val loser = mockk<Player>()
        val loserScore = PlayerScore(PLAYER_B_ID, WINNER_SCORE)
        every { loser.id } returns PLAYER_B_ID
        every { loser.score } returns loserScore

        val winnerScoreData = PlayerScoreData(winner, mockk())
        val loserScoreData = PlayerScoreData(loser, mockk())
        val result = PlayersScoreData(TURN, listOf(winnerScoreData, loserScoreData))

        every { mockReportPlayer(winner) } returns "Player $PLAYER_A_ID: Score=[$WINNER_SCORE]"
        every { mockReportPlayer(loser) } returns "Player $PLAYER_B_ID: Score=[$LOSER_SCORE]"

        // Act
        val report = SUT(result)

        // Assert
        assertEquals(3, report.size)
        assertEquals("Winner $PLAYER_A_ID in $TURN, Score=[$WINNER_SCORE]", report[0])
        assertEquals("Player $PLAYER_A_ID: Score=[$WINNER_SCORE]", report[1])
        assertEquals("Player $PLAYER_B_ID: Score=[$LOSER_SCORE]", report[2])
    }

    @Test
    fun invoke_whenTiedScore_returnsFirstPlayerAsWinner() {
        // Arrange
        val player1 = mockk<Player>()
        val player1Score = PlayerScore(PLAYER_A_ID, WINNER_SCORE)
        every { player1.id } returns PLAYER_A_ID
        every { player1.score } returns player1Score

        val player2 = mockk<Player>()
        val player2Score = PlayerScore(PLAYER_B_ID, WINNER_SCORE)
        every { player2.id } returns PLAYER_B_ID
        every { player2.score } returns player2Score

        val player1ScoreData = PlayerScoreData(player1, mockk())
        val player2ScoreData = PlayerScoreData(player2, mockk())
        val result = PlayersScoreData(TURN, listOf(player1ScoreData, player2ScoreData))

        every { mockReportPlayer(player1) } returns "Player $PLAYER_A_ID: Score=[$WINNER_SCORE]"
        every { mockReportPlayer(player2) } returns "Player $PLAYER_B_ID: Score=[$WINNER_SCORE]"

        // Act
        val report = SUT(result)

        // Assert
        assertEquals(3, report.size)
        assertEquals("Winner $PLAYER_A_ID in $TURN, Score=[$WINNER_SCORE]", report[0])
        assertEquals("Player $PLAYER_A_ID: Score=[$WINNER_SCORE]", report[1])
        assertEquals("Player $PLAYER_B_ID: Score=[$WINNER_SCORE]", report[2])
    }
} 
