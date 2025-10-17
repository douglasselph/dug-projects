package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTime
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShouldProcessMatchEffectTest {
    private val gameTime = mockk<GameTime>(relaxed = true)
    private val SUT get() = ShouldProcessMatchEffect(gameTime)

    private fun card(matchWith: MatchWith, matchEffect: CardEffect): GameCard {
        val card = mockk<GameCard>(relaxed = true)
        every { card.matchWith } returns matchWith
        every { card.matchEffect } returns matchEffect
        return card
    }

    @Test
    fun invoke_whenMatchWithIsNotOnRoll_returnsTrue() {
        val c = card(MatchWith.OnFlourishType(FlourishType.CANOPY), CardEffect.REDUCE_COST_ROOT)
        assertTrue(SUT(c))
    }

    @Test
    fun invoke_whenPhaseIsCultivationAndOnRoll_returnsTrue() {
        every { gameTime.phase } returns GamePhase.CULTIVATION
        val c = card(MatchWith.OnRoll(1), CardEffect.REDUCE_COST_ROOT)
        assertTrue(SUT(c))
    }

    @Test
    fun invoke_whenOnRollAndMatchEffectIsReduceCostRootAndNotCultivation_returnsFalse() {
        every { gameTime.phase } returns GamePhase.BATTLE
        val c = card(MatchWith.OnRoll(1), CardEffect.REDUCE_COST_ROOT)
        assertFalse(SUT(c))
    }

    @Test
    fun invoke_whenOnRollAndMatchEffectIsReduceCostVineAndNotCultivation_returnsFalse() {
        every { gameTime.phase } returns GamePhase.BATTLE
        val c = card(MatchWith.OnRoll(1), CardEffect.REDUCE_COST_VINE)
        assertFalse(SUT(c))
    }

    @Test
    fun invoke_whenOnRollAndMatchEffectIsReduceCostCanopyAndNotCultivation_returnsFalse() {
        every { gameTime.phase } returns GamePhase.BATTLE
        val c = card(MatchWith.OnRoll(1), CardEffect.REDUCE_COST_CANOPY)
        assertFalse(SUT(c))
    }

    @Test
    fun invoke_whenOnRollAndMatchEffectIsFlourishOverrideAndNotCultivation_returnsFalse() {
        every { gameTime.phase } returns GamePhase.BATTLE
        val c = card(MatchWith.OnRoll(1), CardEffect.FLOURISH_OVERRIDE)
        assertFalse(SUT(c))
    }

    @Test
    fun invoke_whenOnRollAndMatchEffectIsNotSpecialCaseAndNotCultivation_returnsTrue() {
        every { gameTime.phase } returns GamePhase.BATTLE
        val c = card(MatchWith.OnRoll(1), CardEffect.ADD_TO_TOTAL)
        assertTrue(SUT(c))
    }
} 
