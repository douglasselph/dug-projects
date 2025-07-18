package dugsolutions.leaf.player.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.AppliedEffect
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class aCardEffectsProcessorTest {

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
    }

    private lateinit var mockCardEffectProcessor: CardEffectProcessor
    private lateinit var mockPlayer: Player
    private lateinit var mockCard1: GameCard
    private lateinit var mockCard2: GameCard
    private lateinit var mockEffect1: AppliedEffect
    private lateinit var mockEffect2: AppliedEffect
    private lateinit var mockEffectsList: DelayedEffectsList
    private lateinit var mockChronicle: GameChronicle

    private lateinit var SUT: CardEffectsProcessor

    @BeforeEach
    fun setup() {
        mockCardEffectProcessor = mockk(relaxed = true)
        mockChronicle = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockCard1 = mockk(relaxed = true) {
            every { id } returns CARD_ID_1
            every { type } returns FlourishType.ROOT
            every { primaryEffect } returns CardEffect.DRAW_CARD
            every { primaryValue } returns 2
        }
        mockCard2 = mockk(relaxed = true) {
            every { id } returns CARD_ID_2
            every { type } returns FlourishType.BLOOM
            every { primaryEffect } returns CardEffect.DRAW_DIE
            every { primaryValue } returns 1
        }
        mockEffect1 = AppliedEffect.DrawCards(2)
        mockEffect2 = AppliedEffect.DrawDice(1)
        mockEffectsList = mockk(relaxed = true)

        SUT = CardEffectsProcessor(
            mockCardEffectProcessor,
            mockChronicle
        )
        every { mockPlayer.delayedEffectList } returns mockEffectsList
    }

    @Test
    fun invoke_whenSingleCard_processesCardAndAddsEffects() = runBlocking {
        // Arrange
        coEvery { mockCardEffectProcessor(mockCard1, mockPlayer) } returns listOf(mockEffect1)

        // Act
        SUT(mockCard1, mockPlayer)

        // Assert
        coVerify { mockCardEffectProcessor(mockCard1, mockPlayer) }
        verify { mockEffectsList.addAll(listOf(mockEffect1)) }
    }

    @Test
    fun invoke_whenMultipleCards_processesAllCardsAndAddsAllEffects() = runBlocking {
        // Arrange
        coEvery { mockCardEffectProcessor(mockCard1, mockPlayer) } returns listOf(mockEffect1)
        coEvery { mockCardEffectProcessor(mockCard2, mockPlayer) } returns listOf(mockEffect2)

        // Act
        for (card in listOf(mockCard1, mockCard2)) {
            SUT(card, mockPlayer)
        }

        // Assert
        coVerify { mockCardEffectProcessor(mockCard1, mockPlayer) }
        coVerify { mockCardEffectProcessor(mockCard2, mockPlayer) }
        verify { mockEffectsList.addAll(listOf(mockEffect1)) }
        verify { mockEffectsList.addAll(listOf(mockEffect2)) }
    }

    @Test
    fun invoke_whenCardHasNoEffects_doesNotAddToEffectsList() = runBlocking {
        // Arrange
        val emptyList = emptyList<AppliedEffect>()
        coEvery { mockCardEffectProcessor(mockCard1, mockPlayer) } returns emptyList

        // Act
        SUT(mockCard1, mockPlayer)

        // Assert
        coVerify { mockCardEffectProcessor(mockCard1, mockPlayer) }
        verify { mockEffectsList.addAll(emptyList) }
    }
} 
