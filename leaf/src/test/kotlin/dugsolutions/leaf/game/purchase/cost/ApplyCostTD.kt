package dugsolutions.leaf.game.purchase.cost

import dugsolutions.leaf.game.purchase.domain.Combination
import dugsolutions.leaf.player.Player
import io.mockk.mockk

class ApplyCostTD : ApplyCost(mockk(relaxed = true)) {
    // Track what was passed into the function
    val gotPlayers = mutableListOf<Player>()
    val gotCombinations = mutableListOf<Combination>()
    val gotCallbacks = mutableListOf<(Player) -> Unit>()

    // Track when the callback was invoked
    var callbackWasInvoked = false

    // Control the response if needed
    var respondWithException: Throwable? = null

    override fun invoke(
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
