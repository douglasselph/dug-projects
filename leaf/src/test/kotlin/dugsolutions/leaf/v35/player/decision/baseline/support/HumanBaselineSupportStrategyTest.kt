package dugsolutions.leaf.v35.player.decision.baseline.support

import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.ChooseButterflyRollRequest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineSupportStrategyTest {
    @Test
    fun `keeps higher visible Butterfly result`() {
        val strategy = HumanBaselineSupportStrategy()
        assertEquals(
            ButterflyRollChoice.ORIGINAL,
            strategy.chooseButterflyRoll(ChooseButterflyRollRequest(12, 9, 4))
        )
        assertEquals(
            ButterflyRollChoice.REROLLED,
            strategy.chooseButterflyRoll(ChooseButterflyRollRequest(12, 3, 8))
        )
    }

    @Test
    fun `does not keep lower one or two for a Roll Reward`() {
        val strategy = HumanBaselineSupportStrategy()

        assertEquals(
            ButterflyRollChoice.ORIGINAL,
            strategy.chooseButterflyRoll(ChooseButterflyRollRequest(8, 6, 1))
        )
        assertEquals(
            ButterflyRollChoice.ORIGINAL,
            strategy.chooseButterflyRoll(ChooseButterflyRollRequest(8, 6, 2))
        )
    }

    @Test
    fun `equal visible results use strategy tie breaker`() {
        val randomizer = RecordingRandomizer(1)
        val strategy = HumanBaselineSupportStrategy(BaselineScoreEngine(randomizer))

        assertEquals(
            ButterflyRollChoice.REROLLED,
            strategy.chooseButterflyRoll(ChooseButterflyRollRequest(8, 5, 5))
        )
        assertEquals(1, randomizer.calls)
        assertEquals(2, randomizer.lastUntil)
    }

    private class RecordingRandomizer(private val result: Int) : StrategyRandomizer {
        var calls = 0
        var lastUntil = 0
        override fun nextInt(until: Int): Int {
            calls++
            lastUntil = until
            return result
        }
    }
}
