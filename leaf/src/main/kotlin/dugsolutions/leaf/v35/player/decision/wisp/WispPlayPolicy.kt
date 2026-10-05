package dugsolutions.leaf.v35.player.decision.wisp

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.domain.WispCard

data class WispPlayObservation(
    val phase: RoundCardType,
    val supportPassNumber: Int? = null,
    val mainActionsRemaining: Int? = null,
    val context: DecisionContext
)

sealed interface WispPlayDecision {
    data object Hold : WispPlayDecision
    data class Play(val card: WispCard) : WispPlayDecision
}

data class ChooseWispPlayRequest(
    val legalCards: List<WispCard>,
    val referenceCard: WispCard?,
    val allowHold: Boolean,
    val observation: WispPlayObservation
) {
    init {
        require(legalCards.isNotEmpty())
        require(referenceCard == null || referenceCard in legalCards)
    }
}

fun interface WispPlayPolicy {
    fun chooseWisp(request: ChooseWispPlayRequest): WispPlayDecision
}

/** Preserves the existing Human choice when it selected a Wisp; otherwise merely exposes a legal Wisp candidate to the enclosing Support policy. */
class HumanWispPlayPolicy : WispPlayPolicy {
    override fun chooseWisp(request: ChooseWispPlayRequest): WispPlayDecision =
        WispPlayDecision.Play(request.referenceCard ?: request.legalCards.first())
}

class MechanicalWispPlayPolicy : WispPlayPolicy {
    override fun chooseWisp(request: ChooseWispPlayRequest): WispPlayDecision =
        WispPlayDecision.Play(request.referenceCard ?: request.legalCards.first())
}
