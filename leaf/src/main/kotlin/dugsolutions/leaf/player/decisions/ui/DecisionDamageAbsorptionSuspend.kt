package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.ui.support.DecisionID
import dugsolutions.leaf.player.decisions.ui.support.DecisionMonitor
import dugsolutions.leaf.player.decisions.ui.support.DecisionSuspensionChannel

class DecisionDamageAbsorptionSuspend(
    monitor: DecisionMonitor
) : DecisionDamageAbsorption {

    private val channel = DecisionSuspensionChannel<DecisionDamageAbsorption.Result>(monitor)

    // region DecisionDamageAbsorption

    override suspend fun invoke(): DecisionDamageAbsorption.Result {
        return channel.waitForDecision(DecisionID.DAMAGE_ABSORPTION)
    }

    // endregion DecisionDamageAbsorption

    // region public

    fun provide(result: DecisionDamageAbsorption.Result) {
        channel.provideDecision(result)
    }

    // endregion public
}
