package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.effect.GameEffect

/** Mechanical delta from one committed Plant resolution; not a causal VP estimate.
 * The same die's pre/post values are paired by object identity.
 * Hand additions count actually drawn dice retained in hand, not attempts.
 */
data class ResearchEffectOutput(
    val playerSeat: Int,
    val plantId: String,
    val phase: ChroniclePhase,
    val effect: GameEffect,
    val immediateVpDelta: Int,
    val existingHandDieValueDelta: Int,
    val newHandDice: Int,
    val newHandDicePips: Int
)
