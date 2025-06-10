package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.player.domain.CardOrDie
import dugsolutions.leaf.random.die.DieSides
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppliedEffectUseCaseTest {

    private lateinit var SUT: AppliedEffectUseCase

    companion object {
        private const val TEST_VALUE = 3
    }

    @BeforeEach
    fun setup() {
        SUT = AppliedEffectUseCase()
    }

    @ParameterizedTest
    @EnumSource(CardEffect::class)
    fun invoke_forAllEffectTypes_returnsCorrectAppliedEffect(effectType: CardEffect) {
        // Act
        val result = SUT(effectType, TEST_VALUE)

        // Assert
        when (effectType) {
            CardEffect.ADD_TO_DIE -> {
                assertTrue(result is AppliedEffect.AdjustDieRoll)
                assertEquals(TEST_VALUE, result.adjustment)
                assertFalse(result.canTargetPlayer)
            }
            CardEffect.ADD_TO_TOTAL -> {
                assertTrue(result is AppliedEffect.AddToTotal)
                assertEquals(TEST_VALUE, result.amount)
            }
            CardEffect.ADJUST_BY -> {
                assertTrue(result is AppliedEffect.AdjustDieRoll)
                assertEquals(TEST_VALUE, result.adjustment)
                assertFalse(result.canTargetPlayer)
            }
            CardEffect.ADJUST_TO_MAX -> {
                assertTrue(result is AppliedEffect.AdjustDieToMax)
                assertFalse(result.minOrMax)
            }
            CardEffect.ADJUST_TO_MIN_OR_MAX -> {
                assertTrue(result is AppliedEffect.AdjustDieToMax)
                assertTrue(result.minOrMax)
            }
            CardEffect.ADORN -> {
                // TODO: Unit test
            }
            CardEffect.DEFLECT -> {
                // TODO: Unit test
            }
            CardEffect.DISCARD -> {
                assertTrue(result is AppliedEffect.Discard)
                assertEquals(TEST_VALUE, result.count)
                assertFalse(result.cardsOnly)
                assertFalse(result.diceOnly)
            }
            CardEffect.DISCARD_CARD -> {
                assertTrue(result is AppliedEffect.Discard)
                assertEquals(TEST_VALUE, result.count)
                assertTrue(result.cardsOnly)
                assertFalse(result.diceOnly)
            }
            CardEffect.DISCARD_DIE -> {
                assertTrue(result is AppliedEffect.Discard)
                assertEquals(TEST_VALUE, result.count)
                assertFalse(result.cardsOnly)
                assertTrue(result.diceOnly)
            }
            CardEffect.DRAW_CARD -> {
                assertTrue(result is AppliedEffect.DrawCards)
                assertEquals(TEST_VALUE, result.count)
                assertFalse(result.fromCompost)
            }
            CardEffect.DRAW_CARD_COMPOST -> {
                assertTrue(result is AppliedEffect.DrawCards)
                assertEquals(TEST_VALUE, result.count)
                assertTrue(result.fromCompost)
            }
            CardEffect.DRAW_DIE -> {
                assertTrue(result is AppliedEffect.DrawDice)
                assertEquals(TEST_VALUE, result.count)
                assertFalse(result.drawHighest)
                assertFalse(result.fromCompost)
            }
            CardEffect.DRAW_DIE_ANY -> {
                assertTrue(result is AppliedEffect.DrawDice)
                assertEquals(TEST_VALUE, result.count)
                assertTrue(result.drawHighest)
                assertFalse(result.fromCompost)
            }
            CardEffect.DRAW_DIE_COMPOST -> {
                assertTrue(result is AppliedEffect.DrawDice)
                assertEquals(TEST_VALUE, result.count)
                assertFalse(result.drawHighest)
                assertTrue(result.fromCompost)
            }
            CardEffect.DRAW_THEN_DISCARD -> {
                assertTrue(result is AppliedEffect.DrawThenDiscard)
                assertEquals(TEST_VALUE, result.drawCount)
                assertEquals(TEST_VALUE - 1, result.discardCount)
            }
            CardEffect.FLOURISH_OVERRIDE -> {
                assertTrue(result is AppliedEffect.FlourishOverride)
            }
            CardEffect.GAIN_FREE_ROOT -> {
                assertTrue(result is AppliedEffect.MarketBenefit)
                assertEquals(FlourishType.ROOT, result.type)
                assertEquals(0, result.costReduction)
                assertTrue(result.isFree)
            }
            CardEffect.GAIN_FREE_CANOPY -> {
                assertTrue(result is AppliedEffect.MarketBenefit)
                assertEquals(FlourishType.CANOPY, result.type)
                assertEquals(0, result.costReduction)
                assertTrue(result.isFree)
            }
            CardEffect.GAIN_FREE_VINE -> {
                assertTrue(result is AppliedEffect.MarketBenefit)
                assertEquals(FlourishType.VINE, result.type)
                assertEquals(0, result.costReduction)
                assertTrue(result.isFree)
            }
            CardEffect.REDUCE_COST_ROOT -> {
                assertTrue(result is AppliedEffect.MarketBenefit)
                assertEquals(FlourishType.ROOT, result.type)
                assertEquals(TEST_VALUE, result.costReduction)
                assertFalse(result.isFree)
            }
            CardEffect.REDUCE_COST_CANOPY -> {
                assertTrue(result is AppliedEffect.MarketBenefit)
                assertEquals(FlourishType.CANOPY, result.type)
                assertEquals(TEST_VALUE, result.costReduction)
                assertFalse(result.isFree)
            }
            CardEffect.REDUCE_COST_VINE -> {
                assertTrue(result is AppliedEffect.MarketBenefit)
                assertEquals(FlourishType.VINE, result.type)
                assertEquals(TEST_VALUE, result.costReduction)
                assertFalse(result.isFree)
            }
            CardEffect.REROLL_ACCEPT_2ND -> {
                assertTrue(result is AppliedEffect.RerollDie)
                assertEquals(TEST_VALUE, result.count)
                assertTrue(result.mustAcceptSecond)
                assertFalse(result.forceMax)
                assertFalse(result.takeBetter)
            }
            CardEffect.REROLL_ALL_MAX -> {
                assertTrue(result is AppliedEffect.RerollDie)
                assertEquals(TEST_VALUE, result.count)
                assertFalse(result.mustAcceptSecond)
                assertTrue(result.forceMax)
                assertFalse(result.takeBetter)
            }
            CardEffect.REROLL_TAKE_BETTER -> {
                assertTrue(result is AppliedEffect.RerollDie)
                assertEquals(TEST_VALUE, result.count)
                assertFalse(result.mustAcceptSecond)
                assertFalse(result.forceMax)
                assertTrue(result.takeBetter)
            }
            CardEffect.RETAIN_CARD -> {
                assertTrue(result is AppliedEffect.RetainCard)
            }
            CardEffect.RETAIN_DIE -> {
                assertTrue(result is AppliedEffect.RetainDie)
                assertFalse(result.withReroll)
            }
            CardEffect.RETAIN_DIE_REROLL -> {
                assertTrue(result is AppliedEffect.RetainDie)
                assertTrue(result.withReroll)
            }
            CardEffect.REUSE_CARD -> {
                assertTrue(result is AppliedEffect.Reuse)
                assertEquals(CardOrDie.Card, result.cardOrDie)
            }
            CardEffect.REUSE_DIE -> {
                assertTrue(result is AppliedEffect.Reuse)
                assertEquals(CardOrDie.Die, result.cardOrDie)
            }
            CardEffect.REUSE_ANY -> {
                assertTrue(result is AppliedEffect.Reuse)
                assertEquals(CardOrDie.Any, result.cardOrDie)
            }
            CardEffect.REPLAY_VINE -> {
                assertTrue(result is AppliedEffect.Replay)
                assertEquals(FlourishType.VINE, result.flourishType)
            }
            CardEffect.UPGRADE_ANY_RETAIN -> {
                assertTrue(result is AppliedEffect.UpgradeDie)
                assertFalse(result.discardAfterUse)
                assertTrue(result.only.isEmpty())
            }
            CardEffect.UPGRADE_ANY -> {
                assertTrue(result is AppliedEffect.UpgradeDie)
                assertTrue(result.discardAfterUse)
                assertTrue(result.only.isEmpty())
            }
            CardEffect.UPGRADE_D4 -> {
                assertTrue(result is AppliedEffect.UpgradeDie)
                assertFalse(result.discardAfterUse)
                assertEquals(listOf(DieSides.D4), result.only)
            }
            CardEffect.UPGRADE_D6 -> {
                assertTrue(result is AppliedEffect.UpgradeDie)
                assertTrue(result.discardAfterUse)
                assertEquals(listOf(DieSides.D4, DieSides.D6), result.only)
            }
            CardEffect.UPGRADE_D8 -> {
                assertTrue(result is AppliedEffect.UpgradeDie)
                assertTrue(result.discardAfterUse)
                assertEquals(listOf(DieSides.D4, DieSides.D6, DieSides.D8), result.only)
            }
            CardEffect.UPGRADE_D10 -> {
                assertTrue(result is AppliedEffect.UpgradeDie)
                assertTrue(result.discardAfterUse)
                assertEquals(listOf(DieSides.D4, DieSides.D6, DieSides.D8, DieSides.D10), result.only)
            }
            CardEffect.USE_OPPONENT_CARD -> {
                assertTrue(result is AppliedEffect.UseOpponent)
                assertEquals(CardOrDie.Card, result.cardOrDie)
            }
            CardEffect.USE_OPPONENT_DIE -> {
                assertTrue(result is AppliedEffect.UseOpponent)
                assertEquals(CardOrDie.Die, result.cardOrDie)
            }
        }
    }

    @Test
    fun invoke_withDrawThenDiscard_calculatesDiscardCountCorrectly() {
        // Arrange
        val drawCount = 5

        // Act
        val result = SUT(CardEffect.DRAW_THEN_DISCARD, drawCount)

        // Assert
        assertTrue(result is AppliedEffect.DrawThenDiscard)
        assertEquals(drawCount, result.drawCount)
        assertEquals(drawCount - 1, result.discardCount)
    }

    @Test
    fun invoke_withDrawThenDiscard_handlesMinimumDrawCount() {
        // Arrange
        val drawCount = 1

        // Act
        val result = SUT(CardEffect.DRAW_THEN_DISCARD, drawCount)

        // Assert
        assertTrue(result is AppliedEffect.DrawThenDiscard)
        assertEquals(drawCount, result.drawCount)
        assertEquals(0, result.discardCount)
    }
} 
