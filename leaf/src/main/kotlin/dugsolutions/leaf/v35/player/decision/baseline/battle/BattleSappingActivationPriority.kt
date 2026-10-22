package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/**
 * The common factual tactical portion of both Sapping activation scoring and
 * downstream die targeting. Targets and activation use the same complete
 * deterministic row projection; an intrinsic Raise/row bonus is NOT added again.
 */
class BattleSappingActivationPriority(
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val projector: BattleSappingSnapdragonProjector = BattleSappingSnapdragonProjector(policy)
) {
    fun isSapping(effect: GameEffect): Boolean = effect ==
        GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW || effect ==
        GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW

    fun bestTacticalValue(context: DecisionContext, effect: GameEffect): Double? {
        if (!isSapping(effect)) return null
        return projector.ownIndices(context).mapNotNull { index ->
            projector.bestForFirst(context, effect, index, index)?.tacticalValue
        }.maxOrNull()
    }

    fun score(context: DecisionContext, effect: GameEffect, base: Int): PriorityScore? =
        bestTacticalValue(context, effect)?.let { tactical ->
            PriorityScore(base).adjusted(tactical.roundToInt(),
                "Best complete Sapping Battle realization (same projection as die targeting)")
        }
}
