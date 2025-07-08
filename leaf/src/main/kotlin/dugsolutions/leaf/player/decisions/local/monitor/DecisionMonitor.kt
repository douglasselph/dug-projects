package dugsolutions.leaf.player.decisions.local.monitor

class DecisionMonitor {

    private val subscribers = mutableListOf<(DecisionID) -> Unit>()
    private var _currentlyWaitingFor: DecisionID? = null

    // region public

    val currentlyWaitingFor: DecisionID? get() = _currentlyWaitingFor

    fun setWaitingFor(id: DecisionID?) {
        _currentlyWaitingFor = id
        if (id != null) {
            // Notify all subscribers that something is waiting
            subscribers.forEach { it(id) }
        }
    }

    // Subscribe to notifications
    fun subscribe(handler: (DecisionID) -> Unit) {
        subscribers.add(handler)
    }

    // endregion public

}
