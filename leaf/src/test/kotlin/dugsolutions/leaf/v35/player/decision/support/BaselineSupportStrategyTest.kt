package dugsolutions.leaf.v35.player.decision.support

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BaselineSupportStrategyTest {

    private val strategy = BaselineSupportStrategy()

    @Test
    fun chooseButterflyRoll_whenRerollHigher_keepsRerolled() {
        assertEquals(
            ButterflyRollChoice.REROLLED,
            strategy.chooseButterflyRoll(
                ChooseButterflyRollRequest(
                    sides = 8,
                    originalValue = 3,
                    rerolledValue = 7
                )
            )
        )
    }

    @Test
    fun chooseButterflyRoll_whenOriginalHigher_keepsOriginal() {
        assertEquals(
            ButterflyRollChoice.ORIGINAL,
            strategy.chooseButterflyRoll(
                ChooseButterflyRollRequest(
                    sides = 8,
                    originalValue = 7,
                    rerolledValue = 3
                )
            )
        )
    }

    @Test
    fun chooseButterflyRoll_whenTied_keepsOriginalDeterministically() {
        assertEquals(
            ButterflyRollChoice.ORIGINAL,
            strategy.chooseButterflyRoll(
                ChooseButterflyRollRequest(
                    sides = 8,
                    originalValue = 5,
                    rerolledValue = 5
                )
            )
        )
    }
}
