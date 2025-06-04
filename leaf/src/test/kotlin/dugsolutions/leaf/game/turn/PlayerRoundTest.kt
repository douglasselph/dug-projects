package dugsolutions.leaf.game.turn

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.turn.handle.HandleCardEffect
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.CardEffectsProcessor
import dugsolutions.leaf.player.effect.CardsEffectsProcessor
import dugsolutions.leaf.player.effect.EffectsList
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerRoundTest {


    private lateinit var mockPlayer: Player
    private lateinit var mockOpponent: Player
    private lateinit var mockCardsEffectsProcessor: CardsEffectsProcessor
    private lateinit var mockHandleCardEffect: HandleCardEffect
    private lateinit var mockEffectsList: EffectsList
    private lateinit var mockCard1: GameCard
    private lateinit var mockCard2: GameCard
    private lateinit var sampleCards: List<GameCard>

    private lateinit var SUT: PlayerRound

    @BeforeEach
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        mockOpponent = mockk(relaxed = true)
        mockCardsEffectsProcessor = mockk(relaxed = true)
        mockHandleCardEffect = mockk(relaxed = true)
        mockEffectsList = mockk(relaxed = true)
        mockCard1 = mockk(relaxed = true)
        mockCard2 = mockk(relaxed = true)
        sampleCards = listOf(mockCard1, mockCard2)

        SUT = PlayerRound(
            cardsEffectsProcessor = mockCardsEffectsProcessor,
            handleCardEffect = mockHandleCardEffect,
        )

        every { mockPlayer.effectsList } returns mockEffectsList
        every { mockEffectsList.clear() } just Runs
        every { mockPlayer.cardsInHand } returns sampleCards
    }

    @Test
    fun invoke_whenCalled_processesEffectsInCorrectOrder() = runBlocking {
        // Act
        SUT(mockPlayer, mockOpponent)

        // Assert
        verify { mockEffectsList.clear() }
        coVerify { mockCardsEffectsProcessor(sampleCards, mockPlayer) }

        verify { mockHandleCardEffect(mockPlayer, mockOpponent) }
    }

    @Test
    fun invoke_whenCalled_processesAllEffects() = runBlocking {
        // Arrange
        // Act
        SUT(mockPlayer, mockOpponent)

        // Assert
        coVerify { mockCardsEffectsProcessor(any(), any()) }
        verify(exactly = 1) { mockHandleCardEffect(any(), any()) }
    }

    @Test
    fun invoke_whenCalled_clearsEffectsListFirst() = runBlocking{
        // Act
        SUT(mockPlayer, mockOpponent)

        // Assert
        coVerifyOrder {
            mockEffectsList.clear()
            mockCardsEffectsProcessor(any(), any())
            mockHandleCardEffect(any(), any())
        }
    }
} 
