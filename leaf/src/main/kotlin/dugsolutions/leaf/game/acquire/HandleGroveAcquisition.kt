package dugsolutions.leaf.game.acquire

import dugsolutions.leaf.game.turn.select.SelectPossibleCards
import dugsolutions.leaf.player.Player

class HandleGroveAcquisition(
    private val selectPossibleCards: SelectPossibleCards,
    private val acquireItem: AcquireItem,
    private val manageAcquiredFloralTypes: ManageAcquiredFloralTypes
) {

    companion object {
        const val FAILSAFE = 1000
    }

    data class NoEndInSiteException(val msg: String) : Exception(msg)

    suspend operator fun invoke(player: Player) {
        manageAcquiredFloralTypes.clear()
        var count = 0
        while (player.diceInHand.isNotEmpty()) {
            if (++count >= FAILSAFE) {
                throw NoEndInSiteException("Grove acquisition loop exceeded maximum iterations ($FAILSAFE). Possible infinite loop detected.")
            }
            val cardsPossible = selectPossibleCards()
            if (!acquireItem(player, cardsPossible)) {
                // Is this something the chronicle should know?
                break
            }
        }
    }

} 
