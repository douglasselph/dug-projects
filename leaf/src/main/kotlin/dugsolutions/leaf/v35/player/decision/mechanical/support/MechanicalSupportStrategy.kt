package dugsolutions.leaf.v35.player.decision.mechanical.support

import dugsolutions.leaf.v35.player.decision.support.*

/** Deterministic mechanical control: keep whichever Butterfly result is higher. */
class MechanicalSupportStrategy : SupportStrategy {
    override fun chooseButterflyRoll(
        request: ChooseButterflyRollRequest
    ): ButterflyRollChoice =
        if (request.rerolledValue > request.originalValue) {
            ButterflyRollChoice.REROLLED
        } else {
            ButterflyRollChoice.ORIGINAL
        }
}
