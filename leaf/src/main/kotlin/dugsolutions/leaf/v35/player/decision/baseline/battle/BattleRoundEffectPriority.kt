package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * C04: contextual, observable Round-effect outcomes. Unlike a Strike-row change,
 * drawing to discard or moving Wisps has no immediate row consequence, so this
 * explicitly returns a NON-TACTICAL provisional preference in legacy points.
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
        GameEffect.GAIN_ONE_VP -> PriorityScore(ORIGIN).adjusted(
            BattleResourceUtility.immediateVp(1), "Immediate non-Strike VP")
        GameEffect.REFRESH_CREATURE -> PriorityScore(ORIGIN).adjusted(0,
            "Refresh benefit requires a projected future Plant activation")
        GameEffect.GAIN_TWO_WORMS -> PriorityScore(ORIGIN).adjusted(0,
            "Worm benefit requires a projected legal Support opportunity")
        else -> PriorityScore(ORIGIN).adjusted(0,
            "Unsupported Round outcome: unprojected, no arbitrary priority premium")
    }

    private fun futureDie(context: DecisionContext, sides: Int): PriorityScore {
        val gain = BattleResourceUtility.futureDiscardDie(context, sides)
        if (gain == 0) return PriorityScore(0).adjusted(0,
            "Die to Discard cannot contribute without another Battle round")
        return PriorityScore(ORIGIN).adjusted(gain,
            "Discounted future Battle draw D$sides; no immediate Strike benefit")
    }
}
