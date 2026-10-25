package dugsolutions.leaf.v35.effect

/**
 * Parameters for the simple effects used in R3B calibration. Both the mechanical
 * executor and the decision projections read these values. They are intentionally
 * keyed by the EFFECT, not a Plant name or a specific player's seat.
 *
 * This is not a general game-tree evaluator: combined effects need their own
 * projections and the rules engine remains authoritative about legal execution.
 */
object SimpleEffectMagnitude {
    fun raiseAmount(effect: GameEffect): Int? = when (effect) {
        GameEffect.RAISE_DIE_PLUS_4 -> 4
        GameEffect.RESEARCH_RAISE_DIE_PLUS_20 -> 20
        else -> null
    }

    fun drawCount(effect: GameEffect): Int? = when (effect) {
        GameEffect.DRAW_TWO_DICE -> 2
        GameEffect.RESEARCH_DRAW_FOUR_DICE -> 4
        else -> null
    }

    fun immediateVp(effect: GameEffect): Int? = when (effect) {
        GameEffect.GAIN_ONE_VP -> 1
        GameEffect.RESEARCH_GAIN_TWENTY_VP -> 20
        else -> null
    }

    fun isResearchNoOp(effect: GameEffect): Boolean =
        effect == GameEffect.RESEARCH_NO_EFFECT
}
