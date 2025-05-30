package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionDrawCount

class DecisionDrawCountSuspend : DecisionDrawCount {

    private val drawCountChannel = DecisionSuspensionChannel<Int>()

    // region DecisionDrawCount

    override suspend operator fun invoke(): Int {
        return drawCountChannel.waitForDecision()
    }

    // endregion DecisionDrawCount

    fun provide(count: Int) {
        drawCountChannel.provideDecision(count)
    }

}
