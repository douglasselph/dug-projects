package dugsolutions.leaf.player.decisions.local.monitor

import kotlinx.coroutines.channels.Channel

class DecisionTaskQueue<Value>(
    private val monitor: DecisionMonitor,
    private val report: DecisionMonitorReport
) {

    private data class DecisionTask<Value>(
        val id: DecisionID
    ) {
        private val channel = Channel<Value>(Channel.CONFLATED)

        suspend fun waitForCompletion(): Value = channel.receive()
        fun complete(value: Value) = channel.trySend(value)
    }

    private val taskQueue = mutableListOf<DecisionTask<Value>>()
    private var currentActiveTask: DecisionTask<Value>? = null

    // region public

    suspend fun waitForDecision(id: DecisionID): Value {
        val task = DecisionTask<Value>(id)
        taskQueue.add(task)
        
        // If this is the first task, make it active
        if (currentActiveTask == null) {
            activateNextTask()
        }
        return task.waitForCompletion()
    }

    fun provideDecision(value: Value) {
        currentActiveTask?.let { task ->
            task.complete(value)
            report(task.id, value)
            currentActiveTask = null
            
            // Activate next task if any
            activateNextTask()
        }
    }

    // endregion public

    private fun activateNextTask() {
        if (taskQueue.isNotEmpty()) {
            currentActiveTask = taskQueue.removeFirst()
            monitor.setWaitingFor(currentActiveTask!!.id) // Notify UI
        }
    }

}
