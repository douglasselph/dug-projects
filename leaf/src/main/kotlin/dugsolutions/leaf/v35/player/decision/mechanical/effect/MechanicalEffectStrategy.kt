package dugsolutions.leaf.v35.player.decision.mechanical.effect

import dugsolutions.leaf.v35.player.decision.effect.*

/** Deterministic first/legal-all mechanical control for effect targeting. */
class MechanicalEffectStrategy : EffectStrategy {
    override fun chooseDie(
        request: ChooseEffectDieRequest
    ): EffectDieChoice = request.legalChoices.first()

    override fun chooseBattleDie(
        request: ChooseEffectBattleDieRequest
    ): EffectBattleDieChoice = request.legalChoices.first()

    override fun chooseCrossPlayerDieSwap(
        request: ChooseEffectCrossPlayerDieSwapRequest
    ): EffectCrossPlayerDieSwapChoice = request.legalChoices.first()

    override fun chooseOptionalDie(
        request: ChooseOptionalEffectDieRequest
    ): EffectDieChoice? = request.legalChoices.firstOrNull()

    override fun chooseDice(
        request: ChooseEffectDiceRequest
    ): EffectDiceChoice = EffectDiceChoice(request.legalChoices.take(1))

    override fun chooseDiePair(
        request: ChooseEffectDiePairRequest
    ): EffectDiePairChoice = request.legalChoices.first()

    override fun chooseOptionalDiePair(
        request: ChooseOptionalEffectDiePairRequest
    ): EffectDiePairChoice? = request.legalChoices.firstOrNull()

    override fun chooseCritterAndDie(
        request: ChooseEffectCritterDieRequest
    ): EffectCritterDieChoice = request.legalChoices.first()

    override fun choosePetalToDie4(
        request: ChoosePetalToDie4Request
    ): PetalToDie4Choice = request.legalChoices.first()

    override fun chooseBeeSource(
        request: ChooseBeeSourceRequest
    ): EffectBeeSourceChoice = request.legalChoices.first()

    override fun chooseButterflyTarget(
        request: ChooseEffectButterflyTargetRequest
    ): EffectButterflyTargetChoice = request.legalChoices.first()

    override fun chooseOptionalPlant(
        request: ChooseOptionalEffectPlantRequest
    ): EffectPlantChoice? = request.legalChoices.firstOrNull()

    override fun chooseOpponentPlantWound(
        request: ChooseEffectOpponentPlantWoundRequest
    ): EffectOpponentPlantWoundChoice = request.legalChoices.first()

    override fun choosePlantEffect(
        request: ChooseEffectPlantRequest
    ): EffectPlantChoice = request.legalChoices.first()

    override fun chooseOEdelweiss(
        request: ChooseOEdelweissRequest
    ): OEdelweissChoice = request.legalChoices.first()

    override fun chooseWispsToKeep(
        request: ChooseWispsToKeepRequest
    ): EffectWispsChoice =
        EffectWispsChoice(
            request.legalChoices.take(
                request.keepLimit
            )
        )

    override fun chooseDieSize(
        request: ChooseEffectDieSizeRequest
    ) = request.legalChoices.first()

    override fun choosePlayer(
        request: ChooseEffectPlayerRequest
    ) = request.legalChoices.first()

    override fun chooseStrikeRow(
        request: ChooseEffectStrikeRowRequest
    ) = request.legalChoices.first()
}
