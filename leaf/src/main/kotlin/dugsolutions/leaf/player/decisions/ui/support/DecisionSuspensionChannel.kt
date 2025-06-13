package dugsolutions.leaf.player.decisions.ui.support

import kotlinx.coroutines.channels.Channel

// A channel-based decision system that can suspend until a decision is made

class DecisionSuspensionChannel<Value>(
    private val monitor: DecisionMonitor
) {
    private val channel = Channel<Value>(Channel.CONFLATED)

    suspend fun waitForDecision(id: DecisionID): Value {
        monitor.setWaitingFor(id)
        val result = channel.receive()
        monitor.setWaitingFor(null)
        return result
    }

    fun provideDecision(value: Value) {
        channel.trySend(value)
    }
}
