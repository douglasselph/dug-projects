package dugsolutions.leaf.game.turn

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.game.acquire.HandleGroveAcquisition
import dugsolutions.leaf.game.battle.HandleDeliverDamage
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.turn.handle.HandleCleanup
import dugsolutions.leaf.game.turn.handle.HandleGetTarget
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.PlayerTD
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerTurnTest {

    private lateinit var mockPlayerRound: PlayerRound
    private lateinit var playerRoundTD: PlayerRoundTD
    private lateinit var mockPlayerOrder: PlayerOrder
    private lateinit var mockHandleDeliverDamage: HandleDeliverDamage
    private lateinit var mockHandleGroveAcquisition: HandleGroveAcquisition
    private lateinit var mockHandleGetTarget: HandleGetTarget
    private lateinit var mockHandleCleanup: HandleCleanup
    private lateinit var mockGameChronicle: GameChronicle

    private lateinit var SUT: PlayerTurn

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
        mockHandleGetTarget = mockk(relaxed = true)
        mockHandleGroveAcquisition = mockk(relaxed = true)
        mockHandleCleanup = mockk(relaxed = true)
        mockGameChronicle = mockk(relaxed = true)

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

        coEvery { mockPlayerRound(any(), any()) } answers {
            val player = firstArg<Player>() as PlayerTD
            val target = secondArg<Player?>() as PlayerTD?
            playerRoundTD(player, target)
        }

        // Create the PlayerTurn instance
        SUT = PlayerTurn(
            mockPlayerRound,
            mockPlayerOrder,
            mockHandleDeliverDamage,
            mockHandleGetTarget,
            mockHandleGroveAcquisition,
            mockHandleCleanup,
            mockGameChronicle
        )

    }

    @Test
    fun invoke_whenCultivationPhase_handlesMarketAcquisition() = runBlocking {
        // Arrange
        // Act
        SUT(players, GamePhase.CULTIVATION)

        // Assert
        coVerify { mockHandleGroveAcquisition(player1) }
        coVerify { mockHandleGroveAcquisition(player2) }
        coVerify { mockHandleGroveAcquisition(player3) }
        coVerify { mockHandleCleanup(player1) }
        coVerify { mockHandleCleanup(player2) }
        coVerify { mockHandleCleanup(player3) }
    }

    @Test
    fun invoke_whenBattlePhase_handlesDamageDelivery() = runBlocking {
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
        coVerify { mockHandleDeliverDamage(players) }
    }

    @Test
    fun invoke_enteringIntoBattlePhase_reordersPlayers() = runBlocking {
        // Arrange
        val reorderedPlayers = listOf(player3, player1, player2)
        every { mockPlayerOrder(any()) } returns players andThen reorderedPlayers

        // Act
        SUT(players, GamePhase.CULTIVATION)

        // Assert
        coVerifyOrder {
            mockHandleGroveAcquisition(player3)
            mockHandleGroveAcquisition(player1)
            mockHandleGroveAcquisition(player2)
        }
    }
}

// region supporting class

private class PlayerRoundTD {

    val attacks = mutableMapOf<Int, Int>()

    operator fun invoke(player: PlayerTD, target: PlayerTD?): Boolean {
        if (player.cardsInHand.isEmpty()) {
            return false
        }
        val card = player.cardsInHand.first()
        player.removeCardFromHand(card)
        if (target != null) {
            if (attacks.containsKey(player.id)) {
                if (attacks[player.id] == target.id) {
                    attacks.remove(player.id)
                }
            }
        }
        return true
    }
}
// endregion
