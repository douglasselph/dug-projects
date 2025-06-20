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
         * If we could not draw at least one card -- then we must draw at least one card before continuing
         * because the rule is that you must already have at least one card.
         */
        if (player.cardsInHand.isEmpty()) {
            player.drawCard()
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
         * At this point we can try again trying to reach the preferred amount of cards the player wants, yet
         * with resupply if needed.
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
        /**
         * Hand all full, we are done.
         */
        if (player.handSize >= HAND_SIZE) {
            return
        }
        /**
         * If at this point there is still room, then just fill up with cards.
         */
        spaceLeft = HAND_SIZE - player.handSize
        repeat(spaceLeft) {
            if (player.handSize < HAND_SIZE) {
                player.drawCard()
            }
        }
    }
}
