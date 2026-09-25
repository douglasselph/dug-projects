package dugsolutions.leaf.v35.game.intervention

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Reusable experiment intervention that replaces one player's opening
 * Cultivation Draw-3 faces during the first N Cultivation rounds.
 *
 * RollResolver has already consumed the natural mechanical RNG result before
 * this policy is consulted. This policy changes only the observed face that
 * continues through Chronicle recording and normal Roll Reward resolution.
 */
class CultivationOpeningDrawFaceIntervention(
    private val affectedPlayerId: PlayerId,
    private val firstCultivationRounds: Int,
    private val forcedFace: Int
) : MechanicalIntervention {

    private val cultivationOpeningRounds = linkedSetOf<Int>()

    init {
        require(firstCultivationRounds > 0) {
            "Cultivation opening intervention requires at least one round"
        }
        require(forcedFace > 0) {
            "Forced die face must be positive: $forcedFace"
        }
    }

    override fun replacementFor(request: MechanicalRollInterventionRequest): Int? {
        if (
            request.source != MechanicalRollSource.CULTIVATION_OPENING_DRAW ||
            request.roundType != RoundCardType.CULTIVATION
        ) {
            return null
        }

        val roundNumber = request.roundNumber ?: return null
        cultivationOpeningRounds += roundNumber

        if (roundNumber !in cultivationOpeningRounds.take(firstCultivationRounds)) {
            return null
        }
        if (request.playerId != affectedPlayerId) {
            return null
        }

        require(forcedFace <= request.sides) {
            "Forced face $forcedFace is invalid for D${request.sides}"
        }
        return forcedFace
    }
}
