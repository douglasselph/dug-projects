package dugsolutions.leaf.game.acquire

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectPossibleCards
import dugsolutions.leaf.player.Player

class HandleGroveAcquisition(
    private val selectPossibleCards: SelectPossibleCards,
    private val acquireItem: AcquireItem,
    private val manageAcquiredFloralTypes: ManageAcquiredFloralTypes,
    private val chronicle: GameChronicle
) {

    companion object {
        const val FAILSAFE = 1000
    }

    data class NoEndInSiteException(val msg: String) : Exception(msg)

    suspend operator fun invoke(player: Player) {
        manageAcquiredFloralTypes.clear()
        var count = 0
        var didAcquireSomething = false
        while (player.diceInHand.isNotEmpty()) {
            if (++count >= FAILSAFE) {
                throw NoEndInSiteException("Grove acquisition loop exceeded maximum iterations ($FAILSAFE). Possible infinite loop detected.")
            }
            val cardsPossible = selectPossibleCards()
            if (acquireItem(player, cardsPossible)) {
                didAcquireSomething = true
            } else {
                break
            }
        }
        if (!didAcquireSomething) {
            chronicle(Moment.ACQUIRE_NONE(player))
        }
    }

} 
