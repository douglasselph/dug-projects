package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.domain.CardEffect
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EffectBattleScoreTest {

    private val SUT = EffectBattleScore()

    @Test
    fun invoke_whenNullEffect_returnsZero() {
        // Act
        val result = SUT(null, 5)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun invoke_whenAddToDieWithLowValue_returnsValue() {
        // Act
        val result = SUT(CardEffect.ADD_TO_DIE, 2)

        // Assert
        assertEquals(2, result)
    }

    @Test
    fun invoke_whenAddToDieWithHighValue_returnsValuePlusOne() {
        // Act
        val result = SUT(CardEffect.ADD_TO_DIE, 4)

        // Assert
        assertEquals(5, result)
    }

    @Test
    fun invoke_whenDeflectWithLowValue_returnsValuePlusOne() {
        // Act
        val result = SUT(CardEffect.DEFLECT, 2)

        // Assert
        assertEquals(3, result)
    }

    @Test
    fun invoke_whenDeflectWithHighValue_returnsValuePlusThree() {
        // Act
        val result = SUT(CardEffect.DEFLECT, 4)

        // Assert
        assertEquals(7, result)
    }

    @Test
    fun invoke_whenAdjustToMax_returnsEighteen() {
        // Act
        val result = SUT(CardEffect.ADJUST_TO_MAX, 5)

        // Assert
        assertEquals(18, result)
    }

    @Test
    fun invoke_whenAdjustToMinOrMax_returnsTwenty() {
        // Act
        val result = SUT(CardEffect.ADJUST_TO_MIN_OR_MAX, 5)

        // Assert
        assertEquals(20, result)
    }

    @Test
    fun invoke_whenDiscard_returnsValueTimesSeven() {
        // Act
        val result = SUT(CardEffect.DISCARD, 3)

        // Assert
        assertEquals(21, result)
    }

    @Test
    fun invoke_whenDrawDieAny_returnsValueTimesFifteen() {
        // Act
        val result = SUT(CardEffect.DRAW_DIE_ANY, 2)

        // Assert
        assertEquals(30, result)
    }

    @Test
    fun invoke_whenFlourishOverride_returnsZero() {
        // Act
        val result = SUT(CardEffect.FLOURISH_OVERRIDE, 5)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun invoke_whenGainFreeVine_returnsValueTimesTwenty() {
        // Act
        val result = SUT(CardEffect.GAIN_FREE_VINE, 2)

        // Assert
        assertEquals(40, result)
    }

    @Test
    fun invoke_whenReduceCostRoot_returnsZero() {
        // Act
        val result = SUT(CardEffect.REDUCE_COST_ROOT, 5)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun invoke_whenRerollAllMax_returnsValueTimesThree() {
        // Act
        val result = SUT(CardEffect.REROLL_ALL_MAX, 2)

        // Assert
        assertEquals(6, result)
    }

    @Test
    fun invoke_whenRetainCard_returnsValueTimesTwo() {
        // Act
        val result = SUT(CardEffect.RETAIN_CARD, 3)

        // Assert
        assertEquals(6, result)
    }

    @Test
    fun invoke_whenReuseAny_returnsValueTimesThirteen() {
        // Act
        val result = SUT(CardEffect.REUSE_ANY, 2)

        // Assert
        assertEquals(26, result)
    }

    @Test
    fun invoke_whenUpgradeAnyRetain_returnsValueTimesFourteen() {
        // Act
        val result = SUT(CardEffect.UPGRADE_ANY_RETAIN, 2)

        // Assert
        assertEquals(28, result)
    }

    @Test
    fun invoke_whenUpgradeD4_returnsValueTimesFour() {
        // Act
        val result = SUT(CardEffect.UPGRADE_D4, 2)

        // Assert
        assertEquals(8, result)
    }
} 
