package dugsolutions.leaf.v35.player.decision.cultivation

/** Deterministic baseline: prefer Draw, otherwise take the first legal action. */
class BaselineCultivationStrategy : CultivationStrategy {
    override fun chooseMainAction(
        request: ChooseCultivationMainActionRequest
    ): CultivationMainAction =
        request.legalChoices.firstOrNull {
            it == CultivationMainAction.Draw
        } ?: request.legalChoices.first()
}
