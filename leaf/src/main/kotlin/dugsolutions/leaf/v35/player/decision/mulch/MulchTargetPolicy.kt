package dugsolutions.leaf.v35.player.decision.mulch

import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy

/**
 * Independent policy family for WHICH Hand die is stored after Mulch has
 * already been selected.  It deliberately does not decide whether to Mulch.
 */
fun interface MulchTargetPolicy {
    fun chooseDie(request: ChooseEffectDieRequest, fallback: EffectStrategy): EffectDieChoice
}

/** Preserve the existing Human Baseline targeting exactly. */
class HumanMulchTargetPolicy : MulchTargetPolicy {
    override fun chooseDie(request: ChooseEffectDieRequest, fallback: EffectStrategy): EffectDieChoice =
        fallback.chooseDie(request)
}
