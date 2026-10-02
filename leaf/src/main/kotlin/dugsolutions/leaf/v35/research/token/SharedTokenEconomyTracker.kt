package dugsolutions.leaf.v35.research.token

import dugsolutions.leaf.v35.tokens.SharedTokenResource
import dugsolutions.leaf.v35.tokens.SharedTokenSupplyObserver

/**
 * Compact game-local accounting for finite Grove component supplies.
 *
 * The tracker is observational only.  Gameplay never consults these values.  A
 * successful Grove withdrawal is counted as a gain; a Grove add is counted as a
 * return/use recycle.  Failed withdrawals are retained separately.  Calling [reset]
 * discards all prior observations, which lets Grove.reset() restore a fresh research
 * baseline without setup mutations becoming gameplay observations.
 */
class SharedTokenEconomyTracker : SharedTokenSupplyObserver {
    private data class MutableState(
        var startingSupply: Int,
        var currentSupply: Int,
        var minimumSupply: Int,
        var gainAttempts: Int = 0,
        var successfulGains: Int = 0,
        var failedEmptyGains: Int = 0,
        var returnsToGrove: Int = 0,
        var timesReachedZero: Int = 0,
        var emptySupplyObservations: Int = 0
    )

    private val states = linkedMapOf<SharedTokenResource, MutableState>()
    private var active = false

    fun reset(startingSupply: Map<SharedTokenResource, Int>) {
        states.clear()
        SharedTokenResource.entries.forEach { resource ->
            val count = startingSupply[resource] ?: 0
            require(count >= 0) { "Starting shared-token supply cannot be negative: $resource=$count" }
            states[resource] = MutableState(
                startingSupply = count,
                currentSupply = count,
                minimumSupply = count
            )
        }
        active = true
    }

    override fun onGainAttempt(resource: SharedTokenResource, success: Boolean) {
        if (!active) return
        val state = states.getValue(resource)
        state.gainAttempts++
        if (success) {
            require(state.currentSupply > 0) {
                "Observed successful $resource Grove withdrawal from empty tracked supply"
            }
            state.successfulGains++
            state.currentSupply--
            if (state.currentSupply < state.minimumSupply) {
                state.minimumSupply = state.currentSupply
            }
            if (state.currentSupply == 0) {
                state.timesReachedZero++
                state.emptySupplyObservations++
            }
        } else {
            if (state.currentSupply == 0) {
                state.failedEmptyGains++
                state.emptySupplyObservations++
            }
        }
    }

    override fun onReturn(resource: SharedTokenResource) {
        if (!active) return
        val state = states.getValue(resource)
        state.returnsToGrove++
        state.currentSupply++
    }

    fun snapshots(): List<SharedTokenEconomySnapshot> =
        SharedTokenResource.entries.map { resource ->
            val state = states.getValue(resource)
            SharedTokenEconomySnapshot(
                resource = resource,
                startingSupply = state.startingSupply,
                gainAttempts = state.gainAttempts,
                successfulGains = state.successfulGains,
                failedEmptyGains = state.failedEmptyGains,
                returnsToGrove = state.returnsToGrove,
                minimumGroveSupply = state.minimumSupply,
                maximumOutsideGrove = state.startingSupply - state.minimumSupply,
                reachedZero = state.minimumSupply == 0,
                timesReachedZero = state.timesReachedZero,
                emptySupplyObservations = state.emptySupplyObservations,
                trackedFinalGroveSupply = state.currentSupply
            )
        }
}

data class SharedTokenEconomySnapshot(
    val resource: SharedTokenResource,
    val startingSupply: Int,
    val gainAttempts: Int,
    val successfulGains: Int,
    val failedEmptyGains: Int,
    val returnsToGrove: Int,
    val minimumGroveSupply: Int,
    val maximumOutsideGrove: Int,
    val reachedZero: Boolean,
    val timesReachedZero: Int,
    val emptySupplyObservations: Int,
    val trackedFinalGroveSupply: Int
)
