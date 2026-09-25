package dugsolutions.leaf.v35.game.intervention

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Explicit experiment-only seam for replacing an already-generated mechanical
 * die result while preserving the normal rules path around that result.
 *
 * The engine always generates the natural result first. An intervention may
 * then return a replacement face. Returning null leaves the natural result
 * unchanged.
 */
fun interface MechanicalIntervention {
    fun replacementFor(request: MechanicalRollInterventionRequest): Int?

    companion object {
        val NONE = MechanicalIntervention { null }
    }
}

/** Creates fresh intervention state for each isolated Game. */
fun interface MechanicalInterventionFactory {
    fun create(): MechanicalIntervention

    companion object {
        val NONE = MechanicalInterventionFactory { MechanicalIntervention.NONE }
    }
}

enum class MechanicalRollSource {
    CULTIVATION_OPENING_DRAW,
    OTHER
}

data class MechanicalRollInterventionRequest(
    val playerId: PlayerId,
    val sides: Int,
    val naturalValue: Int,
    val roundNumber: Int?,
    val roundType: RoundCardType?,
    val source: MechanicalRollSource
)
