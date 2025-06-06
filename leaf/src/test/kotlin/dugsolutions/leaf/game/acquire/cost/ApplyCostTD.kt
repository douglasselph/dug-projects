package dugsolutions.leaf.game.acquire.cost

import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.Player
import io.mockk.mockk

class ApplyCostTD : ApplyCost(
    applyEffects = mockk(relaxed = true)
) {
    // Track what was passed into the function
    val gotPlayers = mutableListOf<Player>()
    val gotCombinations = mutableListOf<Combination>()
    val gotCallbacks = mutableListOf<(Player) -> Unit>()

    // Track when the callback was invoked
    var callbackWasInvoked = false

    // Control the response if needed
    var respondWithException: Throwable? = null

    override operator fun invoke(
        player: Player,
        combination: Combination,
        acquireItem: (player: Player) -> Unit
    ) {
        // Record the invocation
        gotPlayers.add(player)
        gotCombinations.add(combination)
        gotCallbacks.add(acquireItem)

        // Check if we should throw an exception
        respondWithException?.let { throw it }

        // Invoke the callback
        acquireItem(player)

        callbackWasInvoked = true
    }
}
