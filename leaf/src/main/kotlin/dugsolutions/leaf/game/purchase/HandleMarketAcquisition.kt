package dugsolutions.leaf.game.purchase

import dugsolutions.leaf.game.turn.select.SelectPossibleCards
import dugsolutions.leaf.player.Player

class HandleMarketAcquisition(
    private val selectPossibleCards: SelectPossibleCards,
    private val purchaseItem: PurchaseItem,
    private val managePurchasedFloralTypes: ManagePurchasedFloralTypes
) {
    operator fun invoke(player: Player) {
        if (player.isDormant) {
            return
        }
        managePurchasedFloralTypes.clear()
        var count = 0
        while (player.diceInHand.isNotEmpty()) {
            require(++count < 1000) { "Market acquisition loop exceeded maximum iterations (1000). Possible infinite loop detected." }
            val cardsPossible = selectPossibleCards(player)
            purchaseItem(player, cardsPossible)
        }
    }

} 
