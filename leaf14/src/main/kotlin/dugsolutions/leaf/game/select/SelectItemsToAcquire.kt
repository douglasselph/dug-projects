package dugsolutions.leaf.game.select

import dugsolutions.leaf.common.domain.acquire.Choices
import dugsolutions.leaf.player.Player

class SelectItemsToAcquire {

    suspend operator fun invoke(player: Player): Choices {
        return Choices()
    }
}
