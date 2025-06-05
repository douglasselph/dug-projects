package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption

class DecisionDamageAbsorptionSuspend : DecisionDamageAbsorption {

    private val drawCountChannel = DecisionSuspensionChannel<DecisionDamageAbsorption.Result>()

    // region DecisionDamageAbsorption

    override suspend fun invoke(): DecisionDamageAbsorption.Result {
        onDamageAbsorptionRequest()
        return drawCountChannel.waitForDecision()
    }

    // endregion DecisionDamageAbsorption

    // region public

    var onDamageAbsorptionRequest: () -> Unit = {}

    fun provide(result: DecisionDamageAbsorption.Result) {
        drawCountChannel.provideDecision(result)
    }

    // endregion public
}
