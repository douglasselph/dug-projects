package dugsolutions.leaf.v35.player.decision.support

/** Deterministic baseline: keep whichever Butterfly result is higher. */
class BaselineSupportStrategy : SupportStrategy {
    override fun chooseButterflyRoll(
        request: ChooseButterflyRollRequest
    ): ButterflyRollChoice =
        if (request.rerolledValue > request.originalValue) {
            ButterflyRollChoice.REROLLED
        } else {
            ButterflyRollChoice.ORIGINAL
        }
}
