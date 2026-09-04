package dugsolutions.leaf.v35.player.decision.effect

/** Deterministic first-legal-choice baseline for effect targeting. */
class BaselineEffectStrategy : EffectStrategy {
    override fun chooseDie(
        request: ChooseEffectDieRequest
    ): EffectDieChoice = request.legalChoices.first()

    override fun chooseOptionalDie(
        request: ChooseOptionalEffectDieRequest
    ): EffectDieChoice? = request.legalChoices.firstOrNull()
}
