package dugsolutions.leaf.v35.player.decision.effect

/** Deterministic first/legal-all baseline for effect targeting. */
class BaselineEffectStrategy : EffectStrategy {
    override fun chooseDie(
        request: ChooseEffectDieRequest
    ): EffectDieChoice = request.legalChoices.first()

    override fun chooseOptionalDie(
        request: ChooseOptionalEffectDieRequest
    ): EffectDieChoice? = request.legalChoices.firstOrNull()

    override fun chooseDice(
        request: ChooseEffectDiceRequest
    ): EffectDiceChoice = EffectDiceChoice(request.legalChoices.take(1))

    override fun chooseDiePair(
        request: ChooseEffectDiePairRequest
    ): EffectDiePairChoice = request.legalChoices.first()

    override fun chooseCritterAndDie(
        request: ChooseEffectCritterDieRequest
    ): EffectCritterDieChoice = request.legalChoices.first()
}
