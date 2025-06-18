package dugsolutions.leaf.player.decisions.local.monitor

class DecisionMonitor {

    private var _currentlyWaitingFor: DecisionID? = null
    val currentlyWaitingFor: DecisionID? get() = _currentlyWaitingFor

    private val observers = mutableListOf<(DecisionID?) -> Unit>()

    fun setWaitingFor(id: DecisionID?) {
        _currentlyWaitingFor = id
        observers.forEach { it(id) }
    }

    fun observe(observer: (DecisionID?) -> Unit) {
        observers.add(observer)
    }
}
