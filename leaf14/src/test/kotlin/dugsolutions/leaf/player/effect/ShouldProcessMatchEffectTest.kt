package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.domain.Effect
import dugsolutions.leaf.common.domain.GamePhase
import dugsolutions.leaf.common.domain.GameTime
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShouldProcessMatchEffectTest {
    
    private val gameTime = mockk<GameTime>(relaxed = true)
    private val SUT = ShouldProcessMatchEffect(gameTime)

    private fun card(matchEffect: Effect): GameCard {
        val card = mockk<GameCard>(relaxed = true)
        every { card.matchEffect } returns matchEffect
        return card
    }

    @Test
    fun invoke_whenPhaseIsCultivation_returnsTrue() {
        // Arrange
        every { gameTime.phase } returns GamePhase.CULTIVATION
        val card = card(Effect.ADD_TO_DIE)

        // Act
        val result = SUT(card)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenPhaseIsNotCultivationAndAnyEffect_returnsTrue() {
        // Arrange
        every { gameTime.phase } returns GamePhase.BATTLE
        val card = card(Effect.ADD_TO_DIE)

        // Act
        val result = SUT(card)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenPhaseIsNotCultivationAndNoneEffect_returnsTrue() {
        // Arrange
        every { gameTime.phase } returns GamePhase.BATTLE
        val card = card(Effect.NONE)

        // Act
        val result = SUT(card)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenPhaseIsNotCultivationAndGraftDieEffect_returnsTrue() {
        // Arrange
        every { gameTime.phase } returns GamePhase.BATTLE
        val card = card(Effect.GRAFT_DIE)

        // Act
        val result = SUT(card)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenPhaseIsNotCultivationAndRerollAccept2ndEffect_returnsTrue() {
        // Arrange
        every { gameTime.phase } returns GamePhase.BATTLE
        val card = card(Effect.REROLL_ACCEPT_2ND)

        // Act
        val result = SUT(card)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenPhaseIsNotCultivationAndRerollTakeBetterEffect_returnsTrue() {
        // Arrange
        every { gameTime.phase } returns GamePhase.BATTLE
        val card = card(Effect.REROLL_TAKE_BETTER)

        // Act
        val result = SUT(card)

        // Assert
        assertTrue(result)
    }

    @Test
    fun invoke_whenPhaseIsNotCultivationAndUpgradeEffect_returnsTrue() {
        // Arrange
        every { gameTime.phase } returns GamePhase.BATTLE
        val card = card(Effect.UPGRADE)

        // Act
        val result = SUT(card)

        // Assert
        assertTrue(result)
    }
} 
