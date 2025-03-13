package dugsolutions.leaf.game

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.PlayerFactory
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.game.turn.PlayerOrder
import dugsolutions.leaf.game.turn.PlayerTurn
import dugsolutions.leaf.game.turn.config.IsEliminatedNoDiceNorCards
import dugsolutions.leaf.game.turn.config.PlayerBattlePhaseCheck
import dugsolutions.leaf.game.turn.config.PlayerBattlePhaseCheckBloom
import dugsolutions.leaf.game.turn.config.PlayerReadyForBattlePhase
import dugsolutions.leaf.game.turn.config.PlayerSetupForBattlePhase
import dugsolutions.leaf.market.Market
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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

    private lateinit var SUT: Game
    private lateinit var mockPlayerTurn: PlayerTurn
    private lateinit var mockPlayerFactory: PlayerFactory
    private lateinit var mockPlayerOrder: PlayerOrder
    private lateinit var mockPlayerReadyForBattlePhase: PlayerReadyForBattlePhase
    private lateinit var mockSetupForBattlePhase: PlayerSetupForBattlePhase
    private lateinit var mockBattlePhaseCheckBloom: PlayerBattlePhaseCheckBloom
    private lateinit var mockIsEliminated: IsEliminatedNoDiceNorCards
    private lateinit var mockDieFactory: DieFactory
    private lateinit var mockMarket: Market

    private lateinit var mockPlayer1: Player
    private lateinit var mockPlayer2: Player
    private lateinit var mockPlayer3: Player
    private lateinit var mockGameCards: GameCards
    private lateinit var sampleConfig: Game.Config
    private lateinit var mockDie: Die
    private val gameTurn = GameTurn()

    private val fakeSeedlingCard1: GameCard = FakeCards.fakeSeedling
    private val fakeSeedlingCard2: GameCard = FakeCards.fakeSeedling2
    private val fakeSeedlingCard3: GameCard = FakeCards.fakeSeedling3
    private val fakeSeedlingCard4: GameCard = FakeCards.fakeSeedling4

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        mockPlayerTurn = mockk(relaxed = true)
        mockPlayerFactory = mockk(relaxed = true)
        mockPlayerOrder = mockk(relaxed = true)
        mockPlayerReadyForBattlePhase = mockk(relaxed = true)
        mockSetupForBattlePhase = mockk(relaxed = true)
        mockBattlePhaseCheckBloom = mockk(relaxed = true)
        mockIsEliminated = mockk(relaxed = true)
        mockDieFactory = mockk(relaxed = true)
        mockGameCards = mockk(relaxed = true)
        mockMarket = mockk(relaxed = true)
        mockDie = mockk(relaxed = true)

        // Create mock players
        mockPlayer1 = mockk(relaxed = true)
        mockPlayer2 = mockk(relaxed = true)
        mockPlayer3 = mockk(relaxed = true)

        // Setup basic player properties
        every { mockPlayer1.id } returns PLAYER_ID_1
        every { mockPlayer1.name } returns PLAYER_NAME_1
        every { mockPlayer1.bloomCount } returns 0
        every { mockPlayer1.isDormant } returns false
        every { mockPlayer1.bonusDie } returns null

        every { mockPlayer2.id } returns PLAYER_ID_2
        every { mockPlayer2.name } returns PLAYER_NAME_2
        every { mockPlayer2.bloomCount } returns 0
        every { mockPlayer2.isDormant } returns false
        every { mockPlayer2.bonusDie } returns null

        every { mockPlayer3.id } returns PLAYER_ID_3
        every { mockPlayer3.name } returns PLAYER_NAME_3
        every { mockPlayer3.bloomCount } returns 0
        every { mockPlayer3.isDormant } returns false
        every { mockPlayer3.bonusDie } returns null

        // Setup card manager to return seedling cards
        every { mockGameCards.cards } returns listOf(
            fakeSeedlingCard1, fakeSeedlingCard2, fakeSeedlingCard3, fakeSeedlingCard4)

        // Setup player factory to return mock players
        every { mockPlayerFactory(any()) } returns mockPlayer1 andThen mockPlayer2 andThen mockPlayer3

        // Setup player order to return players in the same order
        every { mockPlayerOrder(any()) } returns listOf(mockPlayer1, mockPlayer2, mockPlayer3)

        // Setup isEliminated to return false for all players
        every { mockIsEliminated(any()) } returns false

        sampleConfig = Game.Config(
            numPlayers = NUM_PLAYERS,
            dieFactory = mockDieFactory,
            setup = { _, _ -> },
            isEliminated = mockIsEliminated
        )

        // Create the Game instance
        SUT = Game(
            mockPlayerTurn,
            mockPlayerFactory,
            mockPlayerOrder,
            mockPlayerReadyForBattlePhase,
            mockSetupForBattlePhase,
            gameTurn,
            mockBattlePhaseCheckBloom
        )
        gameTurn.turn = GAME_TURN
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
        gameTurn.turn = GAME_TURN

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
            dieFactory = mockDieFactory,
            setup = { _, _ -> },
            isEliminated = mockIsEliminated
        )
        // Reset expectations
        every { mockPlayerFactory(any()) } returns mockPlayer1 andThen mockPlayer2
        every { mockPlayerOrder(any()) } returns listOf(mockPlayer1, mockPlayer2)

        // Act
        SUT.setup(customConfig)
        
        // Assert
        verify(exactly = 2) { mockPlayerFactory(mockDieFactory) }
        verify { mockPlayerOrder(any()) }
    }

    @Test
    fun setup_callsCustomSetupFunctionForEachPlayer() {
        // Arrange
        val setupFunction = mockk<(Int, Player) -> Unit>(relaxed = true)
        val customConfig = Game.Config(
            numPlayers = 2,
            dieFactory = mockDieFactory,
            setup = setupFunction,
            isEliminated = mockIsEliminated
        )
        // Reset expectations
        every { mockPlayerFactory(any()) } returns mockPlayer1 andThen mockPlayer2
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
            dieFactory = mockDieFactory,
            setup = { _, _ -> },
            isEliminated = mockIsEliminated
        )
        // Reset expectations
        every { mockPlayerFactory(any()) } returns mockPlayer1 andThen mockPlayer2
        every { mockPlayerOrder(any()) } returns listOf(mockPlayer1, mockPlayer2)
        
        // Act
        SUT.setup(customConfig)
        
        // Assert
        verify { mockPlayer1.draw(2) }
        verify { mockPlayer2.draw(2) }
    }

    @Test
    fun setup_customPlayerBattlePhaseCheckIsSet() {
        // Arrange
        val mockCustomBattlePhaseCheck = mockk<PlayerBattlePhaseCheck>(relaxed = true)
        val customConfig = Game.Config(
            numPlayers = 1,
            dieFactory = mockDieFactory,
            setup = { _, _ -> },
            isEliminated = mockIsEliminated,
            playerBattlePhaseCheck = mockCustomBattlePhaseCheck
        )
        // Act
        SUT.setup(customConfig)
        
        // Assert
        assertEquals(mockCustomBattlePhaseCheck, SUT.playerBattlePhaseCheck)
    }

    @Test
    fun runOneCultivationTurn_callsPlayerTurnWithCultivationPhase() {
        // Arrange
        SUT.setup(sampleConfig)

        // Act
        SUT.runOneCultivationTurn()

        // Assert
        verify { mockPlayerTurn(SUT.players, GamePhase.CULTIVATION) }
    }

    @Test
    fun runOneBattleTurn_callsPlayerTurnWithBattlePhase() {
        // Arrange
        SUT.setup(sampleConfig)

        // Act
        SUT.runOneBattleTurn()

        // Assert
        verify { mockPlayerTurn(SUT.players, GamePhase.BATTLE) }
    }

    @Test
    fun detectBattlePhase_whenMostPlayersReady_setsInCultivationPhaseToFalse() {
        // Arrange
        val mockBattlePhaseCheck = mockk<PlayerBattlePhaseCheck>(relaxed = true)
        SUT.playerBattlePhaseCheck = mockBattlePhaseCheck

        every { mockBattlePhaseCheck.isReady(any()) } returns true
        every { mockPlayerReadyForBattlePhase(mockPlayer1, any()) } returns true
        every { mockPlayerReadyForBattlePhase(mockPlayer2, any()) } returns true
        every { mockPlayerReadyForBattlePhase(mockPlayer3, any()) } returns false

        every { mockPlayer1.bonusDie } returns mockDie
        every { mockPlayer2.bonusDie } returns mockDie

        SUT.setup(sampleConfig)

        // Act
        SUT.detectBattlePhase()

        // Assert
        assertFalse(SUT.inCultivationPhase)
    }

    @Test
    fun detectBattlePhase_whenTwoOrMorePlayersNotReady_keepsInCultivationPhaseTrue() {
        // Arrange
        val mockBattlePhaseCheck = mockk<PlayerBattlePhaseCheck>(relaxed = true)
        SUT.playerBattlePhaseCheck = mockBattlePhaseCheck

        every { mockBattlePhaseCheck.isReady(mockPlayer1) } returns true
        every { mockBattlePhaseCheck.isReady(mockPlayer2) } returns false
        every { mockBattlePhaseCheck.isReady(mockPlayer3) } returns false

        every { mockPlayerReadyForBattlePhase(mockPlayer1, true) } returns true
        every { mockPlayerReadyForBattlePhase(mockPlayer2, false) } returns false
        every { mockPlayerReadyForBattlePhase(mockPlayer3, false) } returns false

        SUT.setup(sampleConfig)

        // Act
        SUT.detectBattlePhase()

        // Assert
        assertTrue(SUT.inCultivationPhase)
    }

    @Test
    fun detectBattlePhase_whenTwoOrMorePlayersReady_transitionRemembered() {
        // Arrange
        val mockBattlePhaseCheck = mockk<PlayerBattlePhaseCheck>(relaxed = true)
        SUT.playerBattlePhaseCheck = mockBattlePhaseCheck

        every { mockBattlePhaseCheck.isReady(mockPlayer1) } returns true
        every { mockBattlePhaseCheck.isReady(mockPlayer2) } returns false
        every { mockBattlePhaseCheck.isReady(mockPlayer3) } returns false

        every { mockPlayerReadyForBattlePhase(mockPlayer1, true) } returns true
        every { mockPlayerReadyForBattlePhase(mockPlayer2, false) } returns false
        every { mockPlayerReadyForBattlePhase(mockPlayer3, false) } returns false

        every { mockPlayer1.bonusDie } returns mockDie
        every { mockPlayer2.bonusDie } returns mockDie

        SUT.setup(sampleConfig)

        // Act
        SUT.detectBattlePhase()

        // Assert
        assertTrue(SUT.inCultivationPhase)
    }

    @Test
    fun detectBattlePhase_checksPlayerBattlePhaseCheckReadiness() {
        // Arrange
        val mockBattlePhaseCheck = mockk<PlayerBattlePhaseCheck>(relaxed = true)
        SUT.playerBattlePhaseCheck = mockBattlePhaseCheck
        SUT.setup(sampleConfig)

        // Act
        SUT.detectBattlePhase()

        // Assert
        verify { mockBattlePhaseCheck.isReady(mockPlayer1) }
        verify { mockBattlePhaseCheck.isReady(mockPlayer2) }
        verify { mockBattlePhaseCheck.isReady(mockPlayer3) }
    }

    @Test
    fun detectBattlePhase_correctlyHandlesOddNumberOfPlayers() {
        // Arrange
        val mockBattlePhaseCheck = mockk<PlayerBattlePhaseCheck>(relaxed = true)
        SUT.playerBattlePhaseCheck = mockBattlePhaseCheck

        every { mockBattlePhaseCheck.isReady(any()) } returns false
        every { mockBattlePhaseCheck.isReady(mockPlayer1) } returns true
        every { mockBattlePhaseCheck.isReady(mockPlayer2) } returns true

        every { mockPlayerReadyForBattlePhase(mockPlayer1, true) } returns true
        every { mockPlayerReadyForBattlePhase(mockPlayer2, true) } returns true
        every { mockPlayerReadyForBattlePhase(mockPlayer3, false) } returns false
        SUT.setup(sampleConfig)

        // Act - with 3 players, 2 ready means we move to battle phase (2/3 rounded up is 2)
        SUT.detectBattlePhase()

        // Assert
        assertFalse(SUT.inCultivationPhase)
    }

    @Test
    fun setupBattlePhase_callsPlayerSetupForBattlePhaseForEachPlayer() {
        // Arrange
        val mockBattlePhaseCheck = mockk<PlayerBattlePhaseCheck>(relaxed = true)
        SUT.playerBattlePhaseCheck = mockBattlePhaseCheck
        SUT.setup(sampleConfig)

        // Act
        SUT.setupBattlePhase()

        // Assert
        verify { mockSetupForBattlePhase(mockPlayer1, mockBattlePhaseCheck) }
        verify { mockSetupForBattlePhase(mockPlayer2, mockBattlePhaseCheck) }
        verify { mockSetupForBattlePhase(mockPlayer3, mockBattlePhaseCheck) }
    }

    @Test
    fun setupBattlePhase_callsSetupForEachPlayerInCorrectOrder() {
        // Arrange
        val mockBattlePhaseCheck = mockk<PlayerBattlePhaseCheck>(relaxed = true)
        SUT.playerBattlePhaseCheck = mockBattlePhaseCheck
        SUT.setup(sampleConfig)

        // Act
        SUT.setupBattlePhase()

        // Assert - verify we set up each player in order
        verifyOrder {
            mockSetupForBattlePhase(mockPlayer1, mockBattlePhaseCheck)
            mockSetupForBattlePhase(mockPlayer2, mockBattlePhaseCheck)
            mockSetupForBattlePhase(mockPlayer3, mockBattlePhaseCheck)
        }
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
