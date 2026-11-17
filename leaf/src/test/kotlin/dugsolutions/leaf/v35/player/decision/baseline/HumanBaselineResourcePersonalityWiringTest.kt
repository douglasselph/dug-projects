package dugsolutions.leaf.v35.player.decision.baseline

import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.HumanBaselineCultivationStrategy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class HumanBaselineResourcePersonalityWiringTest {
    @Test
    fun `human baseline without an explicit personality remains neutral`() {
        val wiring = HumanBaselineDecisionDirector()
        assertEquals(ResourcePersonality.NEUTRAL, wiring.policy.resourcePersonality)
        assertSame(wiring.policy, wiring.cultivation.policy)
        assertSame(wiring.policy, wiring.battle.policy)
        assertSame(wiring.policy, wiring.effect.policy)
    }

    @Test
    fun `different players retain distinct personalities through their directors`() {
        val waterPolicy = HumanBaselinePolicy(
            resourcePersonality = ResourcePersonality.WATER_KEEPER
        )
        val battlePolicy = HumanBaselinePolicy(
            resourcePersonality = ResourcePersonality.BATTLE_PREPARER
        )
        val waterDirector = DecisionDirector.humanBaseline(policy = waterPolicy)
        val battleDirector = DecisionDirector.humanBaseline(policy = battlePolicy)
        val waterCultivation = waterDirector.cultivation as HumanBaselineCultivationStrategy
        val battleCultivation = battleDirector.cultivation as HumanBaselineCultivationStrategy

        assertNotSame(waterDirector, battleDirector)
        assertNotSame(waterCultivation, battleCultivation)
        assertSame(waterPolicy, waterCultivation.policy)
        assertSame(battlePolicy, battleCultivation.policy)
        assertEquals(ResourceInclination.LIKE, waterCultivation.policy.resourcePersonality.water.inclination)
        assertEquals(ResourceInclination.NEUTRAL, battleCultivation.policy.resourcePersonality.water.inclination)
        assertEquals(ResourceInclination.LIKE, battleCultivation.policy.resourcePersonality.sunlight.inclination)
        assertEquals(ResourcePersonality.NEUTRAL, HumanBaselinePolicy().resourcePersonality)
    }

    @Test
    fun `mixed profile can be passed without changing the common valuation engine`() {
        val profile = ResourcePersonality.WATER_MULCH_NO_SUNLIGHT
        val wiring = HumanBaselineDecisionDirector(
            policy = HumanBaselinePolicy(resourcePersonality = profile)
        )
        assertSame(profile, wiring.policy.resourcePersonality)
        assertEquals(ResourceInclination.AVOID, wiring.cultivation.policy.resourcePersonality.sunlight.inclination)
        assertSame(wiring.policy, wiring.buy.policy)
    }
}
