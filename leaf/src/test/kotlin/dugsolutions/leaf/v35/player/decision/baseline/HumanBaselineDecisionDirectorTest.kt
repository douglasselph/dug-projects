package dugsolutions.leaf.v35.player.decision.baseline

import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class HumanBaselineDecisionDirectorTest {
    @Test
    fun `all decision areas share one score engine and influence registry`() {
        val wiring = HumanBaselineDecisionDirector()

        assertSame(wiring.scoreEngine, wiring.reward.scoreEngine)
        assertSame(wiring.scoreEngine, wiring.wound.scoreEngine)
        assertSame(wiring.scoreEngine, wiring.placement.scoreEngine)
        assertSame(wiring.scoreEngine, wiring.cultivation.scoreEngine)
        assertSame(wiring.scoreEngine, wiring.battle.scoreEngine)
        assertSame(wiring.scoreEngine, wiring.buy.scoreEngine)
        assertSame(wiring.scoreEngine, wiring.support.scoreEngine)
        assertSame(wiring.scoreEngine, wiring.effect.scoreEngine)

        assertSame(wiring.influenceRegistry, wiring.reward.influenceRegistry)
        assertSame(wiring.influenceRegistry, wiring.wound.influenceRegistry)
        assertSame(wiring.influenceRegistry, wiring.placement.influenceRegistry)
        assertSame(wiring.influenceRegistry, wiring.cultivation.influenceRegistry)
        assertSame(wiring.influenceRegistry, wiring.battle.influenceRegistry)
        assertSame(wiring.influenceRegistry, wiring.buy.influenceRegistry)
        assertSame(wiring.influenceRegistry, wiring.support.influenceRegistry)
        assertSame(wiring.influenceRegistry, wiring.effect.influenceRegistry)

        assertSame(wiring.policy, wiring.cultivation.policy)
        assertSame(wiring.policy, wiring.buy.policy)
    }

    @Test
    fun `created director uses the exact wired strategy instances`() {
        val wiring = HumanBaselineDecisionDirector()
        val director = wiring.createDirector()

        assertSame(wiring.reward, director.reward)
        assertSame(wiring.wound, director.wound)
        assertSame(wiring.placement, director.placement)
        assertSame(wiring.cultivation, director.cultivation)
        assertSame(wiring.battle, director.battle)
        assertSame(wiring.buy, director.buy)
        assertSame(wiring.support, director.support)
        assertSame(wiring.effect, director.effect)
    }
}
