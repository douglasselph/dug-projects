package dugsolutions.leaf.game.turn

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.turn.handle.HandleAdorn
import dugsolutions.leaf.game.turn.handle.HandleCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.PlayerTD
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerRoundTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
        private const val CARD_ID_3 = 3
    }

    private val fakePlayer = PlayerTD.create2(1)
    private val mockOpponent: Player = mockk(relaxed = true)
    private val mockHandleAdorn: HandleAdorn = mockk(relaxed = true)
    private val mockHandleCard: HandleCard = mockk(relaxed = true)
    private val mockCard1: GameCard = mockk(relaxed = true)
    private val mockCard2: GameCard = mockk(relaxed = true)
    private val mockCard3: GameCard = mockk(relaxed = true)
    private lateinit var sampleCards: List<GameCard>

    private val SUT: PlayerRound = PlayerRound(mockHandleCard, mockHandleAdorn)

    @BeforeEach
    fun setup() {
        sampleCards = listOf(mockCard1, mockCard2)

        every { mockCard1.id } returns CARD_ID_1
        every { mockCard2.id } returns CARD_ID_2
        every { mockCard3.id } returns CARD_ID_3

        sampleCards.forEach { fakePlayer.addCardToHand(it) }
    }

    @Test
    fun invoke_whenCalled_clearsEffectsAndAddsCardsToPlay() = runBlocking {
        // Arrange
        fakePlayer.incomingDamage = 2

        // Act
        SUT(fakePlayer, mockOpponent)

        // Assert
        assertEquals(0, fakePlayer.incomingDamage)
        assertEquals(0, fakePlayer.cardsToPlay.size)
    }

    @Test
    fun invoke_whenCardsInHand_processesEachCardInOrder() = runBlocking {
        // Arrange
        // Act
        SUT(fakePlayer, mockOpponent)

        // Assert
        coVerifyOrder {
            mockHandleCard(fakePlayer, mockOpponent, mockCard1)
            mockHandleCard(fakePlayer, mockOpponent, mockCard2)
        }
    }

    @Test
    fun invoke_callHandleAdornFirst() = runBlocking {
        // Arrange
        // Act
        SUT(fakePlayer, mockOpponent)

        // Assert
        coVerifyOrder {
            mockHandleAdorn(fakePlayer)
            mockHandleCard(fakePlayer, mockOpponent, mockCard1)
        }
    }

    @Test
    fun invoke_whenNoCardsInHand_doesNothing() = runBlocking {
        // Arrange
        fakePlayer.removeCardFromHand(mockCard1)
        fakePlayer.removeCardFromHand(mockCard2)

        // Act
        SUT(fakePlayer, mockOpponent)

        // Assert
        coVerify(exactly = 0) { mockHandleCard(any(), any(), any()) }
    }

}
