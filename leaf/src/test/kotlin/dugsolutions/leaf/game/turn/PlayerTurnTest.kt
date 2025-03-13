package dugsolutions.leaf.game.turn

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.game.battle.HandleAbsorbDamage
import dugsolutions.leaf.game.battle.HandleDeliverDamage
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.purchase.HandleMarketAcquisition
import dugsolutions.leaf.game.turn.handle.HandleCleanup
import dugsolutions.leaf.game.turn.handle.HandleGetTarget
import dugsolutions.leaf.game.turn.handle.HandlePassOrPlay
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.PlayerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PlayerTurnTest {

    private lateinit var SUT: PlayerTurn

    private lateinit var mockPlayerRound: PlayerRound
    private lateinit var playerRoundTD: PlayerRoundTD
    private lateinit var mockPlayerOrder: PlayerOrder
    private lateinit var mockHandleDeliverDamage: HandleDeliverDamage
    private lateinit var handlePlayOrPass: HandlePassOrPlay
    private lateinit var mockHandleMarketAcquisition: HandleMarketAcquisition
    private lateinit var mockHandleAbsorbDamage: HandleAbsorbDamage
    private lateinit var mockHandleGetTarget: HandleGetTarget
    private lateinit var mockHandleCleanup: HandleCleanup
    
    private lateinit var player1: PlayerTD
    private lateinit var player2: PlayerTD
    private lateinit var player3: PlayerTD
    private lateinit var players: List<Player>

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        playerRoundTD = PlayerRoundTD()
        mockPlayerRound = mockk(relaxed = true)
        mockPlayerOrder = mockk(relaxed = true)
        mockHandleDeliverDamage = mockk(relaxed = true)
        handlePlayOrPass = HandlePassOrPlay()
        mockHandleGetTarget = mockk(relaxed = true)
        mockHandleMarketAcquisition = mockk(relaxed = true)
        mockHandleAbsorbDamage = mockk(relaxed = true)
        mockHandleCleanup = mockk(relaxed = true)
        
        // Create mock players
        player1 = PlayerTD(1)
        player2 = PlayerTD(2)
        player3 = PlayerTD(3)

        val card1 = FakeCards.fakeBloom
        val card2 = FakeCards.fakeSeedling
        val card3 = FakeCards.fakeRoot

        player1.addCardToHand(card1)
        player2.addCardToHand(card2)
        player3.addCardToHand(card3)

        // Setup basic player properties
        
        players = listOf(player1, player2, player3)

        every { mockHandleGetTarget(player1, players) } returns player2
        every { mockHandleGetTarget(player2, players) } returns player3
        every { mockHandleGetTarget(player3, players) } returns player1

        // Setup player order to return players in the same order
        every { mockPlayerOrder(any()) } returns players

        every { mockPlayerRound(any(), any()) } answers {
            val player = firstArg<Player>() as PlayerTD
            val target = secondArg<Player?>() as PlayerTD?
            playerRoundTD(player, target)
        }

        // Create the PlayerTurn instance
        SUT = PlayerTurn(
            mockPlayerRound,
            mockPlayerOrder,
            mockHandleDeliverDamage,
            handlePlayOrPass,
            mockHandleGetTarget,
            mockHandleMarketAcquisition,
            mockHandleAbsorbDamage,
            mockHandleCleanup
        )
    }

    @Test
    fun invoke_whenCultivationPhase_handlesMarketAcquisition() {
        // Arrange
        // Act
        SUT(players, GamePhase.CULTIVATION)
        
        // Assert
        verify { mockHandleMarketAcquisition(player1) }
        verify { mockHandleMarketAcquisition(player2) }
        verify { mockHandleMarketAcquisition(player3) }
        verify { mockHandleCleanup(player1) }
        verify { mockHandleCleanup(player2) }
        verify { mockHandleCleanup(player3) }
    }

    @Test
    fun invoke_whenBattlePhase_handlesDamageDelivery() {
        // Arrange
        val card1 = FakeCards.fakeBloom
        val card2 = FakeCards.fakeSeedling
        val card3 = FakeCards.fakeRoot

        player1.addCardToHand(card1)
        player2.addCardToHand(card2)
        player3.addCardToHand(card3)

        // Act
        SUT(players, GamePhase.BATTLE)
        
        // Assert
        verify { mockHandleDeliverDamage(players) }
        verify { mockHandleAbsorbDamage(player1) }
        verify { mockHandleAbsorbDamage(player2) }
        verify { mockHandleAbsorbDamage(player3) }
    }

    @Test
    fun invoke_hitsClearedAtTheStartOfEachRound() {
        // Arrange
        players.forEach { it.wasHit = true }
        
        // Act
        SUT(players, GamePhase.CULTIVATION)
        
        // Assert
        players.forEach { assertFalse(it.wasHit) }
    }

    @Test
    fun invoke_clearsPassedBeforeStarting() {
        // Arrange
        players.forEach { it.hasPassed = true }

        // Act
        SUT(players, GamePhase.CULTIVATION)
        
        // Assert
        verify { mockHandleGetTarget(player1, players) }
        verify { mockHandleGetTarget(player2, players) }
        verify { mockHandleGetTarget(player3, players) }
    }

    @Test
    fun invoke_enteringIntoBattlePhase_reordersPlayers() {
        // Arrange
        val reorderedPlayers = listOf(player3, player1, player2)
        every { mockPlayerOrder(any()) } returns players andThen reorderedPlayers
        
        // Act
        SUT(players, GamePhase.CULTIVATION)
        
        // Assert
        verifyOrder {
            mockHandleMarketAcquisition(player3)
            mockHandleMarketAcquisition(player1)
            mockHandleMarketAcquisition(player2)
        }
    }
}

// region supporting class

private class PlayerRoundTD {

    val attacks = mutableMapOf<Int,Int>()

    operator fun invoke(player: PlayerTD, target: PlayerTD?): Boolean {
        if (player.cardsInHand.isEmpty()) {
            player.hasPassed = true
            return false
        }
        val card = player.cardsInHand.first()
        player.removeCardFromHand(card)
        if (target != null) {
            if (attacks.containsKey(player.id)) {
                if (attacks[player.id] == target.id) {
                    attacks.remove(player.id)
                    target.wasHit = true
                }
            }
        }
        return true
    }
}
// endregion
