package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player

class HandleCleanup {

    suspend operator fun invoke(player: Player) {
        // Discard remaining hand
        player.discardHand()

        // Cards reused
        player.cardsReused.forEach { card ->
            player.addCardToHand(card.id)
        }
        player.cardsReused.clear()

        // If empty, resupply
        if (player.diceInSupplyCount == 0 && player.cardsInSupplyCount == 0) {
            player.resupply()
        }
        player.drawHand()
    }

}
