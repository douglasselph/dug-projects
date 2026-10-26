package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.projection.SharedDieEffectOutcome
import dugsolutions.leaf.v35.player.decision.context.DieView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class C02SharedDieEffectOutcomeTest {
    private val dice = listOf(
        DieView(index = 0, sides = 4, value = 3),
        DieView(index = 1, sides = 6, value = 2),
        DieView(index = 2, sides = 12, value = 9)
    )

    @Test fun actionProjectionEqualsBestTargetProjection() {
        val effects = listOf(
            GameEffect.DOUBLE_ONE_DIE,
            GameEffect.RAISE_DIE_PLUS_4,
            GameEffect.RESEARCH_RAISE_DIE_PLUS_20,
            GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE,
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX
        )
        for (effect in effects) {
            val legal = if (effect == GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX)
                dice.filter { it.value == dice.minOf { die -> die.value } } else dice
            assertEquals(legal.maxOf { SharedDieEffectOutcome.projectedPips(effect, it)!! },
                SharedDieEffectOutcome.bestProjectedPips(effect, dice), effect.name)
        }
    }

    @Test fun projectionsRespectPhysicalDieCaps() {
        assertEquals(1, SharedDieEffectOutcome.projectedPips(GameEffect.RAISE_DIE_PLUS_4, 4, 3))
        assertEquals(3, SharedDieEffectOutcome.projectedPips(GameEffect.RESEARCH_RAISE_DIE_PLUS_20, 12, 9))
        assertEquals(4, SharedDieEffectOutcome.projectedPips(GameEffect.RAISE_DIE_PLUS_4, 6, 2))
    }

    @Test fun unsupportedEffectsAreNotMisrepresentedAsZero() {
        assertNull(SharedDieEffectOutcome.projectedPips(GameEffect.GAIN_SUNLIGHT_TOKEN, 6, 2))
        assertNull(SharedDieEffectOutcome.bestProjectedPips(GameEffect.RAISE_D8_PLUS_1, dice))
    }
}
