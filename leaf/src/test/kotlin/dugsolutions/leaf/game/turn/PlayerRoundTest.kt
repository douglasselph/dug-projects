package dugsolutions.leaf.game.turn

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.turn.handle.HandleDrawEffect
import dugsolutions.leaf.game.turn.handle.HandleLocalCardEffect
import dugsolutions.leaf.game.turn.handle.HandleOpponentEffects
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.CardEffectsProcessor
import dugsolutions.leaf.player.effect.EffectsList
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerRoundTest {

    private lateinit var SUT: PlayerRound

    private lateinit var mockPlayer: Player
    private lateinit var mockOpponent: Player
    private lateinit var mockCardEffectsProcessor: CardEffectsProcessor
    private lateinit var mockHandleDrawEffect: HandleDrawEffect
    private lateinit var mockHandleLocalCardEffect: HandleLocalCardEffect
    private lateinit var mockHandleOpponentEffects: HandleOpponentEffects
    private lateinit var mockEffectsList: EffectsList
    private lateinit var mockCard1: GameCard
    private lateinit var mockCard2: GameCard
    private lateinit var sampleCards: List<GameCard>

    @BeforeEach
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        mockOpponent = mockk(relaxed = true)
        mockCardEffectsProcessor = mockk(relaxed = true)
        mockHandleDrawEffect = mockk(relaxed = true)
        mockHandleLocalCardEffect = mockk(relaxed = true)
        mockHandleOpponentEffects = mockk(relaxed = true)
        mockEffectsList = mockk(relaxed = true)
        mockCard1 = mockk(relaxed = true)
        mockCard2 = mockk(relaxed = true)
        sampleCards = listOf(mockCard1, mockCard2)

        SUT = PlayerRound(
            cardEffectsProcessor = mockCardEffectsProcessor,
            handleDrawEffect = mockHandleDrawEffect,
            handleLocalCardEffect = mockHandleLocalCardEffect,
            handleOpponentEffects = mockHandleOpponentEffects
        )

        every { mockPlayer.effectsList } returns mockEffectsList
        every { mockEffectsList.clear() } just Runs
        every { mockPlayer.cardsInHand } returns sampleCards
        every { mockPlayer.cardsToPlay } returns sampleCards.toMutableList()
        every { mockCardEffectsProcessor(any(), any()) } just Runs
        every { mockHandleDrawEffect(mockPlayer) } just Runs
        every { mockHandleLocalCardEffect(mockPlayer) } just Runs
        every { mockHandleOpponentEffects(mockPlayer, mockOpponent) } just Runs
    }

    @Test
    fun invoke_whenCalled_processesEffectsInCorrectOrder() {
        // Act
        SUT(mockPlayer, mockOpponent)

        // Assert
        verify { mockEffectsList.clear() }
        for (card in sampleCards) {
            verify { mockCardEffectsProcessor(card, mockPlayer) }
        }
        verify { mockHandleDrawEffect(mockPlayer) }
        verify { mockHandleLocalCardEffect(mockPlayer) }
        verify { mockHandleOpponentEffects(mockPlayer, mockOpponent) }
    }

    @Test
    fun invoke_whenCalled_processesAllEffects() {
        // Arrange
        // Act
        SUT(mockPlayer, mockOpponent)

        // Assert
        verify { mockCardEffectsProcessor(any(), any()) }
        verify(exactly = 1) { mockHandleDrawEffect(any()) }
        verify(exactly = 1) { mockHandleLocalCardEffect(any()) }
        verify(exactly = 1) { mockHandleOpponentEffects(any(), any()) }
    }

    @Test
    fun invoke_whenCalled_clearsEffectsListFirst() {
        // Act
        SUT(mockPlayer, mockOpponent)

        // Assert
        verifyOrder {
            mockEffectsList.clear()
            mockCardEffectsProcessor(any(), any())
            mockHandleDrawEffect(any())
            mockHandleLocalCardEffect(any())
            mockHandleOpponentEffects(any(), any())
        }
    }
} 
