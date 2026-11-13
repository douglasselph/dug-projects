package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/**
 * C04: contextual, observable Round-effect outcomes. Most resource effects
 * (Discard dice and Wisps) have no immediate Strike consequence. Swell is an
 * exception: its new immediate-use die is scored with the Strike projector.
 * A zero available gain must never receive the former high fixed score.
 * These values are not represented as BattleConsequenceValue or Strike VP.
 */
object BattleRoundEffectPriority {
    private const val ORIGIN = BattleResourceUtility.ORIGIN

    fun score(context: DecisionContext, effect: GameEffect): PriorityScore = when (effect) {
        GameEffect.GAIN_ONE_WISP -> {
            val available = BattleResourceUtility.availableWispGain(context)
            if (available == 0) PriorityScore(0).adjusted(0, "No Wisp can be acquired")
            else PriorityScore(ORIGIN).adjusted(BattleResourceUtility.wisps(available),
                "One available Wisp: future effect/VP option, not a Strike award")
        }
        GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT,
        GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS -> {
            val count = BattleResourceUtility.opponentWispTransfers(context,
                effect == GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS)
            if (count == 0) PriorityScore(0).adjusted(0, "No transferable opponent Wisps")
            else PriorityScore(ORIGIN).adjusted(
                BattleResourceUtility.wisps(count) + BattleResourceUtility.deniedWisps(count),
                "Transfer $count Wisps: future option plus opponent denial, not Strike VP")
        }
        GameEffect.GAIN_D20_TO_DISCARD -> futureDie(context, 20)
        GameEffect.GAIN_D12_TO_DISCARD -> futureDie(context, 12)
        GameEffect.GAIN_D10_TO_DISCARD -> futureDie(context, 10)
        GameEffect.GAIN_ANY_DIE_TO_DISCARD -> futureDie(context, 20)
        GameEffect.GAIN_UP_TO_D12_TO_DISCARD -> boundedDiscardDie(context, 12)
        GameEffect.GAIN_UP_TO_D8_AND_USE_NOW -> boundedDieUsedNow(context, 8)
        GameEffect.GAIN_ONE_VP -> PriorityScore(ORIGIN).adjusted(
            BattleResourceUtility.immediateVp(1), "Immediate non-Strike VP")
        GameEffect.REFRESH_CREATURE -> recovery(BattleRecoveryResourceUtility.refresh(context))
        GameEffect.GAIN_TWO_WORMS -> recovery(BattleRecoveryResourceUtility.worms(context))
        else -> PriorityScore(ORIGIN).adjusted(0,
            "Unsupported Round outcome: unprojected, no arbitrary priority premium")
    }

    private fun boundedDiscardDie(context: DecisionContext, maxSides: Int): PriorityScore {
        val best = context.grove.graftBed
            .filter { (sides, count) -> sides.value <= maxSides && count > 0 }
            .keys.maxByOrNull { it.value }
            ?: return PriorityScore(0).adjusted(0, "No available Graft Bed die up to D$maxSides")
        return futureDie(context, best.value)
    }

    private fun boundedDieUsedNow(context: DecisionContext, maxSides: Int): PriorityScore {
        val best = context.grove.graftBed
            .filter { (sides, count) -> sides.value <= maxSides && count > 0 }
            .keys.maxByOrNull { it.value }
            ?: return PriorityScore(0).adjusted(0, "No available Graft Bed die up to D$maxSides")
        val expected = (best.value + 1) / 2.0
        // Unlike a gained Discard die, Swell contributes to the current Strike.
        // The same tactical projector used for a normal Battle Draw accounts for
        // the Strike outcome, including VP, only once.
        val projection = BattleConsequenceEvaluator()
            .placedDie(context, expected, BattleAnalysisMode.EXPECTED)
            .filter { it.legal }
            .maxByOrNull { it.tacticalPreference }
            ?: return PriorityScore(0).adjusted(0, "No open Strike Square for gained D${best.value}")
        val intrinsic = (expected + BattleDrawRollRewardUtility.expected(context, best.value)).roundToInt()
        return PriorityScore(ORIGIN).adjusted(
            intrinsic + projection.tacticalPreference.roundToInt(),
            "Swell D${best.value}: immediate rolled die, available rewards and projected Strike impact"
        )
    }

    private fun recovery(opportunity: BattleRecoveryResourceUtility.Opportunity): PriorityScore =
        if (!opportunity.available) PriorityScore(0).adjusted(0, opportunity.explanation)
        else PriorityScore(ORIGIN).adjusted(opportunity.increment, opportunity.explanation)

    private fun futureDie(context: DecisionContext, sides: Int): PriorityScore {
        val gain = BattleResourceUtility.futureDiscardDie(context, sides)
        if (gain == 0) return PriorityScore(0).adjusted(0,
            "Die to Discard cannot contribute without another Battle round")
        return PriorityScore(ORIGIN).adjusted(gain,
            "Discounted future Battle draw D$sides; no immediate Strike benefit")
    }
}
