package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionDrawCount

class DecisionDrawCountSuspend : DecisionDrawCount {

    private val channel = DecisionSuspensionChannel<Int>()

    // region DecisionDrawCount

    override suspend operator fun invoke(): Int {
        onDrawCountRequest()
        return channel.waitForDecision()
    }

    // endregion DecisionDrawCount

    // region public

    var onDrawCountRequest: () -> Unit = {}

    fun provide(count: Int) {
        channel.provideDecision(count)
    }

    // endregion public
}
