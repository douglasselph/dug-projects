package dugsolutions.leaf.game

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.player.di.PlayerFactory
import dugsolutions.leaf.game.battle.BattlePhaseTransition
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.game.turn.PlayerOrder
import dugsolutions.leaf.game.turn.PlayerTurn
import dugsolutions.leaf.game.turn.config.IsEliminatedNoDiceNorCards
import dugsolutions.leaf.game.turn.handle.HandleDrawHand
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameTest {

    companion object {
        private const val PLAYER_ID_1 = 1
        private const val PLAYER_ID_2 = 2
        private const val PLAYER_ID_3 = 3
        private const val PLAYER_NAME_1 = "Player 1"
        private const val PLAYER_NAME_2 = "Player 2"
        private const val PLAYER_NAME_3 = "Player 3"
        private const val NUM_PLAYERS = 3
        private const val GAME_TURN = 2
    }

    private val mockPlayerTurn: PlayerTurn = mockk(relaxed = true)
    private lateinit var mockPlayerFactory: PlayerFactory
    private lateinit var mockPlayerOrder: PlayerOrder
    private lateinit var mockIsEliminated: IsEliminatedNoDiceNorCards
    private lateinit var mockDieFactory: DieFactory
    private lateinit var mockGrove: Grove
    private lateinit var mockBattlePhaseTransition: BattlePhaseTransition
    private val mockHandleDrawHand: HandleDrawHand = mockk(relaxed = true)

    private lateinit var mockPlayer1: Player
    private lateinit var mockPlayer2: Player
    private lateinit var mockPlayer3: Player
    private lateinit var mockGameCards: GameCards
    private lateinit var sampleConfig: Game.Config
    private lateinit var mockDie: Die
    private val gameTime = GameTime()

    private lateinit var SUT: Game

    private val fakeSeedlingCard1: GameCard = FakeCards.seedlingCard
    private val fakeSeedlingCard2: GameCard = FakeCards.seedlingCard2
    private val fakeSeedlingCard3: GameCard = FakeCards.seedlingCard3
    private val fakeSeedlingCard4: GameCard = FakeCards.seedlingCard4

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        mockPlayerFactory = mockk(relaxed = true)
        mockPlayerOrder = mockk(relaxed = true)
        mockIsEliminated = mockk(relaxed = true)
        mockDieFactory = mockk(relaxed = true)
        mockGameCards = mockk(relaxed = true)
        mockGrove = mockk(relaxed = true)
        mockDie = mockk(relaxed = true)
        mockBattlePhaseTransition = mockk(relaxed = true)

        // Create mock players
        mockPlayer1 = mockk(relaxed = true)
        mockPlayer2 = mockk(relaxed = true)
        mockPlayer3 = mockk(relaxed = true)

        // Setup basic player properties
        every { mockPlayer1.id } returns PLAYER_ID_1
        every { mockPlayer1.name } returns PLAYER_NAME_1

        every { mockPlayer2.id } returns PLAYER_ID_2
        every { mockPlayer2.name } returns PLAYER_NAME_2

        every { mockPlayer3.id } returns PLAYER_ID_3
        every { mockPlayer3.name } returns PLAYER_NAME_3

        // Setup card manager to return seedling cards
        every { mockGameCards.cards } returns listOf(
            fakeSeedlingCard1, fakeSeedlingCard2, fakeSeedlingCard3, fakeSeedlingCard4
        )

        // Setup player factory to return mock players
        every { mockPlayerFactory() } returns mockPlayer1 andThen mockPlayer2 andThen mockPlayer3

        // Setup player order to return players in the same order
        every { mockPlayerOrder(any()) } returns listOf(mockPlayer1, mockPlayer2, mockPlayer3)

        // Setup isEliminated to return false for all players
        every { mockIsEliminated(any()) } returns false

        sampleConfig = Game.Config(
            numPlayers = NUM_PLAYERS,
            setup = { _, _ -> },
            isEliminated = mockIsEliminated
        )

        // Create the Game instance
        SUT = Game(
            mockPlayerTurn,
            mockPlayerFactory,
            mockPlayerOrder,
            mockHandleDrawHand,
            mockGrove,
            gameTime,
            mockBattlePhaseTransition,
        )
        gameTime.turn = GAME_TURN
    }

    @Test
    fun score_returnsCorrectPlayersScoreData() {
        // Arrange
        val score1 = PlayerScore(10, 5, 2)
        val score2 = PlayerScore(20, 8, 3)
        val score3 = PlayerScore(15, 6, 4)

        every { mockPlayer1.score } returns score1
        every { mockPlayer2.score } returns score2
        every { mockPlayer3.score } returns score3

        SUT.setup(sampleConfig)
        gameTime.turn = GAME_TURN

        // Act
        val result = SUT.score

        // Assert
        assertEquals(GAME_TURN, result.turn)
        assertEquals(3, result.players.size)
        assertEquals(score1, result.players[0].score)
        assertEquals(score2, result.players[1].score)
        assertEquals(score3, result.players[2].score)
    }

    @Test
    fun setup_initializesCorrectNumberOfPlayers() {
        // Arrange
        val customConfig = Game.Config(
            numPlayers = 2,
            setup = { _, _ -> },
            isEliminated = mockIsEliminated
        )
        // Reset expectations
        every { mockPlayerFactory() } returns mockPlayer1 andThen mockPlayer2
        every { mockPlayerOrder(any()) } returns listOf(mockPlayer1, mockPlayer2)

        // Act
        SUT.setup(customConfig)

        // Assert
        verify(exactly = 2) { mockPlayerFactory() }
        verify { mockPlayerOrder(any()) }
    }

    @Test
    fun setup_callsCustomSetupFunctionForEachPlayer() {
        // Arrange
        val setupFunction = mockk<(Int, Player) -> Unit>(relaxed = true)
        val customConfig = Game.Config(
            numPlayers = 2,
            setup = setupFunction,
            isEliminated = mockIsEliminated
        )
        // Reset expectations
        every { mockPlayerFactory() } returns mockPlayer1 andThen mockPlayer2
        every { mockPlayerOrder(any()) } returns listOf(mockPlayer1, mockPlayer2)

        // Act
        SUT.setup(customConfig)

        // Assert
        verify { setupFunction(0, mockPlayer1) }
        verify { setupFunction(1, mockPlayer2) }
    }

    @Test
    fun setup_playersDrawInitialCards() {
        // Arrange
        val customConfig = Game.Config(
            numPlayers = 2,
            setup = { _, _ -> },
            isEliminated = mockIsEliminated
        )
        // Reset expectations
        every { mockPlayerFactory() } returns mockPlayer1 andThen mockPlayer2
        every { mockPlayerOrder(any()) } returns listOf(mockPlayer1, mockPlayer2)

        // Act
        SUT.setup(customConfig)

        // Assert
        verify { mockHandleDrawHand(mockPlayer1, 2) }
        verify { mockHandleDrawHand(mockPlayer2, 2) }
    }

    @Test
    fun runOneCultivationTurn_callsPlayerTurnWithCultivationPhase() = runBlocking {
        // Arrange
        SUT.setup(sampleConfig)

        // Act
        SUT.runOneCultivationTurn()

        // Assert
        coVerify { mockPlayerTurn(SUT.players, GamePhase.CULTIVATION) }
    }

    @Test
    fun runOneBattleTurn_callsPlayerTurnWithBattlePhase() = runBlocking {
        // Arrange
        SUT.setup(sampleConfig)

        // Act
        SUT.runOneBattleTurn()

        // Assert
        coVerify { mockPlayerTurn(SUT.players, GamePhase.BATTLE) }
    }

    @Test
    fun isGameFinished_whenNoPlayersRemaining_returnsTrue() {
        // Arrange
        every { mockIsEliminated(mockPlayer1) } returns true
        every { mockIsEliminated(mockPlayer2) } returns true
        every { mockIsEliminated(mockPlayer3) } returns true
        SUT.setup(sampleConfig)

        // Act
        val result = SUT.isGameFinished

        // Assert
        assertTrue(result)
    }

    @Test
    fun isGameFinished_whenOnePlayerRemaining_returnsTrue() {
        // Arrange
        every { mockIsEliminated(mockPlayer1) } returns true
        every { mockIsEliminated(mockPlayer2) } returns false
        every { mockIsEliminated(mockPlayer3) } returns true
        SUT.setup(sampleConfig)

        // Act
        val result = SUT.isGameFinished

        // Assert
        assertTrue(result)
    }

    @Test
    fun isGameFinished_whenMultiplePlayersRemaining_returnsFalse() {
        // Arrange
        every { mockIsEliminated(mockPlayer1) } returns false
        every { mockIsEliminated(mockPlayer2) } returns false
        every { mockIsEliminated(mockPlayer3) } returns true
        SUT.setup(sampleConfig)

        // Act
        val result = SUT.isGameFinished

        // Assert
        assertFalse(result)
    }
} 
