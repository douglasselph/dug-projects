package dugsolutions.leaf.v35.effect

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class B03RetiredSunlightRejectionTest {
    private val converter = GameEffectConverter()

    @Test
    fun officialSunlightWordingStillConvertsToToken() {
        assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, converter("Gain Sunlight token", "RoundCard"))
    }

    @Test
    fun oldHumanReadableSunlightEffectFailsClosedRatherThanMappingToToken() {
        for (old in listOf("🎲 Raise a die +3", "Raise a die +3", "Raise any die +3", "RAISE_DIE_PLUS_3")) {
            val exception = assertFailsWith<IllegalArgumentException> {
                converter(old, "Resource_Sunlight_Water/effect_1")
            }
            assertTrue(exception.message.orEmpty().contains("Obsolete Sunlight Raise +3"))
            assertTrue(exception.message.orEmpty().contains("Resource_Sunlight_Water"))
        }
    }

    @Test
    fun oldHandlerIsNotAvailableButModernRaiseFourIsUnaffected() {
        val executor = DefaultGameEffectExecutor()
        assertFalse(executor.supports(GameEffect.RAISE_DIE_PLUS_3))
        assertTrue(executor.supports(GameEffect.RAISE_DIE_PLUS_4))
        assertTrue(executor.supports(GameEffect.GAIN_SUNLIGHT_TOKEN))
    }
}
