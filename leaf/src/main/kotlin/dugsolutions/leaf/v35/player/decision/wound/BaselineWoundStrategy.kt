package dugsolutions.leaf.v35.player.decision.wound

/**
 * Deterministic baseline wound policy: choose the first legal option.
 *
 * The Wound resolver is responsible for constructing the legal choices
 * according to v35 rules:
 *
 * - if any grafted Plant card is face up, offer Flip choices
 * - otherwise, offer Snip choices for snippable outer cards
 */
class BaselineWoundStrategy : WoundStrategy {

    override fun choose(
        request: ChooseWoundRequest
    ): WoundChoice =
        request.legalChoices.first()
}
