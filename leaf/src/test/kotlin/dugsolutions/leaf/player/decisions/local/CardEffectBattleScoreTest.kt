package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.FloralBonusCount
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CardEffectBattleScoreTest {

    private lateinit var mockPlayer: Player
    private lateinit var mockEffectBattleScore: EffectBattleScore
    private lateinit var mockFloralBonusCount: FloralBonusCount
    private lateinit var SUT: CardEffectBattleScore

    @BeforeEach
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        mockEffectBattleScore = mockk(relaxed = true)
        mockFloralBonusCount = mockk(relaxed = true)
        SUT = CardEffectBattleScore(mockPlayer, mockEffectBattleScore, mockFloralBonusCount)
    }

    @Test
    fun invoke_whenNoEffects_returnsZero() {
        // Arrange
        val card = mockk<GameCard>(relaxed = true) {
            every { primaryEffect } returns null
            every { matchEffect } returns null
            every { trashEffect } returns null
        }

        // Act
        val result = SUT(card)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun invoke_whenHasPrimaryEffect_returnsPrimaryScore() {
        // Arrange
        val card = mockk<GameCard>(relaxed = true) {
            every { primaryEffect } returns CardEffect.ADD_TO_DIE
            every { primaryValue } returns 2
            every { matchEffect } returns null
            every { trashEffect } returns null
        }
        every { mockEffectBattleScore(CardEffect.ADD_TO_DIE, 2) } returns 2

        // Act
        val result = SUT(card)

        // Assert
        assertEquals(2, result)
    }

    @Test
    fun invoke_whenHasMatchEffectWithFlower_returnsCombinedScore() {
        // Arrange
        val flowerCard = FakeCards.fakeFlower
        val card = mockk<GameCard>(relaxed = true) {
            every { primaryEffect } returns null
            every { matchWith } returns MatchWith.Flower(flowerCard.id)
            every { matchEffect } returns CardEffect.ADD_TO_DIE
            every { matchValue } returns 2
            every { trashEffect } returns null
        }
        every { mockPlayer.floralCards } returns listOf(flowerCard)
        every { mockPlayer.allCardsInDeck } returns listOf(flowerCard)
        every { mockEffectBattleScore(CardEffect.ADD_TO_DIE, 2) } returns 2
        every { mockFloralBonusCount(any(), flowerCard.id) } returns 1

        // Act
        val result = SUT(card)

        // Assert
        assertEquals(6, result) // 2 * (1 + 1) * 1.5 = 6
    }

    @Test
    fun invoke_whenHasMatchEffectWithOnFlourishType_returnsCombinedScore() {
        // Arrange
        val card = mockk<GameCard>(relaxed = true) {
            every { primaryEffect } returns null
            every { matchWith } returns MatchWith.OnFlourishType(FlourishType.BLOOM)
            every { matchEffect } returns CardEffect.ADD_TO_DIE
            every { matchValue } returns 2
            every { trashEffect } returns null
        }
        every { mockPlayer.allCardsInDeck } returns listOf(
            mockk { every { type } returns FlourishType.BLOOM },
            mockk { every { type } returns FlourishType.BLOOM },
            mockk { every { type } returns FlourishType.VINE }
        )
        every { mockEffectBattleScore(CardEffect.ADD_TO_DIE, 2) } returns 2

        // Act
        val result = SUT(card)

        // Assert
        assertEquals(8, result) // 2 * (2/3) * 6 = 8
    }

    @Test
    fun invoke_whenHasMatchEffectWithOnRoll_returnsCombinedScore() {
        // Arrange
        val card = mockk<GameCard>(relaxed = true) {
            every { primaryEffect } returns null
            every { matchWith } returns MatchWith.OnRoll(1)
            every { matchEffect } returns CardEffect.ADD_TO_DIE
            every { matchValue } returns 10
            every { trashEffect } returns null
        }
        every { mockEffectBattleScore(CardEffect.ADD_TO_DIE, 10) } returns 10

        // Act
        val result = SUT(card)

        // Assert
        assertEquals(2, result) // 10 / 5 = 2
    }

    @Test
    fun invoke_whenHasTrashEffect_returnsCombinedScore() {
        // Arrange
        val card = mockk<GameCard>(relaxed = true) {
            every { primaryEffect } returns null
            every { matchEffect } returns null
            every { trashEffect } returns CardEffect.ADD_TO_DIE
            every { trashValue } returns 8
        }
        every { mockEffectBattleScore(CardEffect.ADD_TO_DIE, 8) } returns 8

        // Act
        val result = SUT(card)

        // Assert
        assertEquals(2, result) // 8 / 4 = 2
    }

    @Test
    fun invoke_whenHasAllEffects_returnsTotalScore() {
        // Arrange
        val card = mockk<GameCard>(relaxed = true) {
            every { primaryEffect } returns CardEffect.ADD_TO_DIE
            every { primaryValue } returns 2
            every { matchWith } returns MatchWith.OnRoll(1)
            every { matchEffect } returns CardEffect.ADD_TO_DIE
            every { matchValue } returns 10
            every { trashEffect } returns CardEffect.ADD_TO_DIE
            every { trashValue } returns 8
        }
        every { mockEffectBattleScore(CardEffect.ADD_TO_DIE, 2) } returns 2
        every { mockEffectBattleScore(CardEffect.ADD_TO_DIE, 10) } returns 10
        every { mockEffectBattleScore(CardEffect.ADD_TO_DIE, 8) } returns 8

        // Act
        val result = SUT(card)

        // Assert
        assertEquals(6, result) // 2 + (10/5) + (8/4) = 2 + 2 + 2 = 6
    }
} 
