package dugsolutions.leaf.v35.player.decision.cultivation

/**
 * Deterministic baseline:
 * - prefer a Main Draw while Main Actions remain
 * - otherwise take the first offered Main Action
 * - once both Main Actions are spent, finish rather than using optional support
 * - fall back to the first legal choice
 */
class BaselineCultivationStrategy : CultivationStrategy {
    override fun chooseAction(
        request: ChooseCultivationActionRequest
    ): CultivationAction {
        request.legalChoices
            .filterIsInstance<CultivationAction.Main>()
            .firstOrNull { it.action == CultivationMainAction.Draw }
            ?.let { return it }

        request.legalChoices
            .filterIsInstance<CultivationAction.Main>()
            .firstOrNull()
            ?.let { return it }

        if (CultivationAction.Done in request.legalChoices) {
            return CultivationAction.Done
        }

        return request.legalChoices.first()
    }
}
