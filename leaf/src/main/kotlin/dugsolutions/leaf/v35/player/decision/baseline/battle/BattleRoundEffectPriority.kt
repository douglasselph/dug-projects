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
    private const val WISP_CAP = 5
    private const val ORIGIN = 35

    fun score(context: DecisionContext, effect: GameEffect): PriorityScore = when (effect) {
        GameEffect.GAIN_ONE_WISP -> {
            val available = context.grove.wispDeckRemaining > 0 && context.self.wispCount < WISP_CAP
            PriorityScore(ORIGIN).adjusted(if (available) 15 else -35,
                if (available) "Available Wisp from Grove" else "No Wisp can be acquired")
        }
        GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT,
        GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS -> {
            val eligible = context.opponents.count { it.wispCount > 0 }
            val gainCapacity = (WISP_CAP - context.self.wispCount).coerceAtLeast(0)
            val possible = if (effect == GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT)
                minOf(1, eligible, gainCapacity) else minOf(eligible, gainCapacity)
            PriorityScore(ORIGIN).adjusted(
                if (possible == 0) -35 else possible * 12,
                "Wisp transfer: $possible available opponent Wisps, self capacity $gainCapacity"
            )
        }
        GameEffect.GAIN_D20_TO_DISCARD -> futureDie(context, 20)
        GameEffect.GAIN_D12_TO_DISCARD -> futureDie(context, 12)
        GameEffect.GAIN_D10_TO_DISCARD -> futureDie(context, 10)
        GameEffect.GAIN_ANY_DIE_TO_DISCARD -> futureDie(context, 20)
        // Other Round effects retain their existing provisional legacy priorities.
        GameEffect.GAIN_ONE_VP -> PriorityScore(60)
        GameEffect.REFRESH_CREATURE -> PriorityScore(55)
        GameEffect.GAIN_TWO_WORMS -> PriorityScore(55)
        else -> PriorityScore(45)
    }

    private fun futureDie(context: DecisionContext, sides: Int): PriorityScore {
        // A die deposited in discard cannot change a Strike on the current turn.
        // No future rounds means no projected spending benefit from this acquisition.
        val rounds = context.progress.roundsRemainingToReveal
        if (rounds <= 0) return PriorityScore(ORIGIN).adjusted(-35,
            "Die to discard has no remaining rounds to be redrawn")
        val expectation = (sides + 1) / 2.0
        return PriorityScore(ORIGIN).adjusted((expectation * 2).toInt(),
            "Future draw value for D$sides; no immediate Battle Strike benefit")
    }
}
