package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.round.domain.RoundCardType

/** Phase-normalized card scoring context. */
enum class CardPhase {
    CULTIVATION,
    BATTLE;

    companion object {
        fun from(type: RoundCardType?): CardPhase =
            if (type == RoundCardType.BATTLE) BATTLE else CULTIVATION
    }
}
