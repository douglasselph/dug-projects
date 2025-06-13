package dugsolutions.leaf.player.decisions.ui.support

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment

class DecisionMonitor(
    private val chronicle: GameChronicle
) {

    private var _currentlyWaitingFor: DecisionID? = null
    val currentlyWaitingFor: DecisionID? get() = _currentlyWaitingFor

    private val observers = mutableListOf<(DecisionID?) -> Unit>()

    fun setWaitingFor(id: DecisionID?) {
        _currentlyWaitingFor = id
        chronicle(Moment.INFO("DEBUG: setWaitingFor($id)"))
        observers.forEach { it(id) }
    }

    fun observe(observer: (DecisionID?) -> Unit) {
        observers.add(observer)
    }
}
