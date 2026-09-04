package dugsolutions.leaf.v35.error

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class GameExceptionsTest {

    @Test
    fun decisionCheck_throwsTypedExceptionWithSpecificReason() {
        val thrown =
            assertFailsWith<InvalidDecisionException> {
                decisionCheck(false) {
                    "Strategy selected face-down Plant while a face-up Plant exists"
                }
            }

        assertEquals("Decision", thrown.context)
        assertEquals(
            "Strategy selected face-down Plant while a face-up Plant exists",
            thrown.reason
        )
        assertEquals(
            "[Decision] Strategy selected face-down Plant while a face-up Plant exists",
            thrown.message
        )
    }

    @Test
    fun stateCheck_throwsInvalidGameStateException() {
        val thrown =
            assertFailsWith<InvalidGameStateException> {
                stateCheck(false) {
                    "Validated D4 could not be returned to the Graft Bed"
                }
            }

        assertEquals("GameState", thrown.context)
    }

    @Test
    fun effectCheck_throwsEffectExecutionException() {
        assertFailsWith<EffectExecutionException> {
            effectCheck(false) {
                "Effect is not executable in the current phase"
            }
        }
    }

    @Test
    fun lifecycleCheck_throwsGameLifecycleException() {
        assertFailsWith<GameLifecycleException> {
            lifecycleCheck(false) {
                "Game cannot start from COMPLETE"
            }
        }
    }

    @Test
    fun unsupportedGameEffect_isSpecializedEffectException() {
        val thrown =
            assertFailsWith<UnsupportedGameEffectException> {
                unsupportedGameEffect(
                    "Unsupported effect reached DrawEffectHandler"
                )
            }

        assertIs<EffectExecutionException>(thrown)
    }
}
