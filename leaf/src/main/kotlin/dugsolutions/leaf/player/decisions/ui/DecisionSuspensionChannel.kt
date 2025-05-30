package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import kotlinx.coroutines.channels.Channel

// A channel-based decision system that can suspend until a decision is made
class DecisionSuspensionChannel<T> {

    private val channel = Channel<T>(Channel.CONFLATED)

    suspend fun waitForDecision(): T {
        return channel.receive()
    }

    fun provideDecision(decision: T) {
        channel.trySend(decision)
    }

}
