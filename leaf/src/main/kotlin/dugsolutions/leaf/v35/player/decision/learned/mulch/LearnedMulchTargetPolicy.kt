package dugsolutions.leaf.v35.player.decision.learned.mulch

import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.mulch.MulchTargetPolicy

class LearnedMulchTargetPolicy(private val weights: LearnedMulchTargetWeights) : MulchTargetPolicy {
    override fun chooseDie(request: ChooseEffectDieRequest, fallback: EffectStrategy): EffectDieChoice {
        val human = fallback.chooseDie(request)
        return request.legalChoices.maxWithOrNull(
            compareBy<EffectDieChoice> { candidate ->
                val f = MulchTargetFeatureExtractor.extract(request, candidate, human)
                MulchTargetFeature.entries.sumOf { f[it.ordinal] * weights[it] }
            }.thenBy { it.index }
        ) ?: error("Mulch Target learner received no legal dice")
    }
}
