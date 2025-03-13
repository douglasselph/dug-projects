package dugsolutions.leaf.game.turn.local

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.AppliedEffect
import dugsolutions.leaf.player.effect.EffectsList
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardIsFreeTest {
    companion object {
        private const val PLAYER_ID = 1
        private const val PLAYER_NAME = "Test Player"
    }

    private lateinit var SUT: CardIsFree
    private lateinit var player: Player
    private lateinit var effectsList: EffectsList

    @BeforeEach
    fun setup() {
        // Create mock player
        player = mockk()
        every { player.id } returns PLAYER_ID
        every { player.name } returns PLAYER_NAME
        
        // Create mock effects list
        effectsList = mockk()
        every { player.effectsList } returns effectsList
        
        // Create CardIsFree instance
        SUT = CardIsFree()
    }

    @Test
    fun invoke_whenNoEffects_returnsFalse() {
        // Arrange
        val card = FakeCards.fakeRoot
        every { effectsList.iterator() } returns emptyList<AppliedEffect>().iterator()

        // Act
        val result = SUT(card, player)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenEffectMakesCardFree_returnsTrue() {
        // Arrange
        val card = FakeCards.fakeRoot
        val effect = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            isFree = true,
            costReduction = 0
        )
        every { effectsList.iterator() } returns listOf(effect).iterator()

        // Act
        val result = SUT(card, player)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenEffectReducesCostOnly_returnsFalse() {
        // Arrange
        val card = FakeCards.fakeRoot
        val effect = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            isFree = false,
            costReduction = 1
        )
        every { effectsList.iterator() } returns listOf(effect).iterator()

        // Act
        val result = SUT(card, player)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenEffectForDifferentType_returnsFalse() {
        // Arrange
        val card = FakeCards.fakeRoot
        val effect = AppliedEffect.MarketBenefit(
            type = FlourishType.BLOOM, // Different type
            isFree = true,
            costReduction = 0
        )
        every { effectsList.iterator() } returns listOf(effect).iterator()

        // Act
        val result = SUT(card, player)

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenMultipleEffects_findsFreeEffect() {
        // Arrange
        val card = FakeCards.fakeRoot
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

        every { effectsList.iterator() } returns listOf(effect1, effect2, effect3).iterator()

        // Act
        val result = SUT(card, player)

        // Assert
        assertTrue(result) // Should use effect2 which makes the card free
    }

    @Test
    fun invoke_whenNonMarketBenefitEffect_returnsFalse() {
        // Arrange
        val card = FakeCards.fakeRoot
        val effect = AppliedEffect.ThornEffect(damage = 1)
        every { effectsList.iterator() } returns listOf(effect).iterator()

        // Act
        val result = SUT(card, player)

        // Assert
        assertFalse(result)
    }
} 
