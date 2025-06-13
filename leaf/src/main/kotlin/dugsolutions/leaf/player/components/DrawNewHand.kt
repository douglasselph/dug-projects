package dugsolutions.leaf.player.components

import dugsolutions.leaf.common.Commons.HAND_SIZE
import dugsolutions.leaf.player.Player

class DrawNewHand(

) {

    operator fun invoke(player: Player, preferredCardCount: Int) {

        repeat(preferredCardCount) {
            if (player.handSize >= HAND_SIZE) {
                return
            }
            player.drawCard()
        }
        val spaceLeft = HAND_SIZE - player.handSize
    }
}
