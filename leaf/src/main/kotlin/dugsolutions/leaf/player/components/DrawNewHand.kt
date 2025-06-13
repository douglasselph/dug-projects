package dugsolutions.leaf.player.components

import dugsolutions.leaf.common.Commons.HAND_SIZE
import dugsolutions.leaf.player.Player
import kotlin.math.max

class DrawNewHand {

    operator fun invoke(player: Player, preferredCardCount: Int) {
        /**
         * First draw cards, without resupply, as many times as desired.
         */
        repeat(preferredCardCount) {
            if (player.handSize < HAND_SIZE) {
                player.drawCardWithoutResupply()
            }
        }
        /**
         * Now draw dice with the remaining space in the player's hand, without resupply
         */
        var spaceLeft = HAND_SIZE - player.handSize
        repeat(spaceLeft) {
            if (player.handSize < HAND_SIZE) {
                player.drawDieWithoutResupply()
            }
        }
        /**
         * Hand all full, we are done.
         */
        if (player.handSize >= HAND_SIZE) {
            return
        }
        /**
         * Otherwise we need to resupply. So try again computing the remaining cards that are still desired.
         */
        val cardsLeftToDraw = max(0, preferredCardCount - player.cardsInHand.size)
        repeat(cardsLeftToDraw) {
            if (player.handSize < HAND_SIZE) {
                player.drawCard()
            }
        }
        /**
         * Now anything left must be dice, since that is all we could possible have.
         */
        spaceLeft = HAND_SIZE - player.handSize
        repeat(spaceLeft) {
            if (player.handSize < HAND_SIZE) {
                player.drawDie()
            }
        }
    }
}
