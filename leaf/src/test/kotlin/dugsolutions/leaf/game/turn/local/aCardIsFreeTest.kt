package dugsolutions.leaf.game.turn.local

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.AppliedEffect
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class aCardIsFreeTest {
    companion object {
        private const val PLAYER_ID = 1
        private const val PLAYER_NAME = "Test Player"
    }

    private val player: Player = mockk(relaxed = true)
    private val effectsList = mutableListOf<AppliedEffect>()

    private lateinit var SUT: CardIsFree

    @BeforeEach
    fun setup() {
        // Create mock player
        every { player.id } returns PLAYER_ID
        every { player.name } returns PLAYER_NAME
        
        // Create mock effects list
        every { player.delayedEffectList } returns effectsList
        
        // Create CardIsFree instance
        SUT = CardIsFree()
    }

    @Test
    fun invoke_whenNoEffects_returnsFalse() {
        // Arrange
        val card = FakeCards.rootCard

        // Act
        val result = SUT(card, player)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenEffectMakesCardFree_returnsTrue() {
        // Arrange
        val card = FakeCards.rootCard
        val effect = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            isFree = true,
            costReduction = 0
        )
        effectsList.add(effect)

        // Act
        val result = SUT(card, player)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenEffectReducesCostOnly_returnsFalse() {
        // Arrange
        val card = FakeCards.rootCard
        val effect = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            isFree = false,
            costReduction = 1
        )
        effectsList.add(effect)

        // Act
        val result = SUT(card, player)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenEffectForDifferentType_returnsFalse() {
        // Arrange
        val card = FakeCards.rootCard
        val effect = AppliedEffect.MarketBenefit(
            type = FlourishType.BLOOM, // Different type
            isFree = true,
            costReduction = 0
        )
        effectsList.add(effect)

        // Act
        val result = SUT(card, player)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenMultipleEffects_findsFreeEffect() {
        // Arrange
        val card = FakeCards.rootCard
        val effect1 = AppliedEffect.MarketBenefit(
            type = FlourishType.BLOOM, // Different type
            isFree = true,
            costReduction = 0
        )
        val effect2 = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT, // Matching type
            isFree = true,
            costReduction = 0
        )
        val effect3 = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT, // Matching type but not free
            isFree = false,
            costReduction = 1
        )
        effectsList.addAll(listOf(effect1, effect2, effect3))

        // Act
        val result = SUT(card, player)

        // Assert
        assertTrue(result) // Should use effect2 which makes the card free
    }

    @Test
    fun invoke_whenNonMarketBenefitEffect_returnsFalse() {
        // Arrange
        val card = FakeCards.rootCard
        val effect = AppliedEffect.FlourishOverride
        effectsList.add(effect)

        // Act
        val result = SUT(card, player)

        // Assert
        assertFalse(result)
    }
} 
