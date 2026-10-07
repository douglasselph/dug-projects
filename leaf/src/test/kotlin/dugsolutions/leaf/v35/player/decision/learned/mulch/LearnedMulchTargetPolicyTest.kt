package dugsolutions.leaf.v35.player.decision.learned.mulch

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.mulch.HumanMulchTargetPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class LearnedMulchTargetPolicyTest {
    private val lowD20 = EffectDieChoice(0, 20, 1)
    private val highD6 = EffectDieChoice(1, 6, 6)
    private val request = ChooseEffectDieRequest(GameEffect.MULCH_DIE_FROM_HAND, listOf(lowD20, highD6))
    private val fallback = object : EffectStrategy {
        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice = highD6
    }

    @Test fun `human mulch target remains exact fallback`() {
        assertEquals(highD6, HumanMulchTargetPolicy().chooseDie(request, fallback))
    }

    @Test fun `learned target scores every candidate independent of list order`() {
        val a = DoubleArray(MulchTargetFeature.entries.size)
        a[MulchTargetFeature.DIE_REROLL_GAIN.ordinal] = 10.0
        val policy = LearnedMulchTargetPolicy(LearnedMulchTargetWeights.fromDoubleArray(a))
        assertEquals(lowD20, policy.chooseDie(request, fallback))
        val reversed = ChooseEffectDieRequest(GameEffect.MULCH_DIE_FROM_HAND, listOf(highD6, lowD20))
        assertEquals(lowD20, policy.chooseDie(reversed, fallback))
    }
}
