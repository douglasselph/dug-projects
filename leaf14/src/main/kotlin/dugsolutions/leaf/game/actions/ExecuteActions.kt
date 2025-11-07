package dugsolutions.leaf.game.actions

import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player

class ExecuteActions(
    private val executeAction: ExecuteAction,
    private val grove: Grove
) {

    suspend operator fun invoke(player: Player) {
        val availableCards = grove.getTopShowingCards()
        val availableDice = grove.getAvailableDiceSides()
        val availableBugs = grove.getAvailableInsects()

        while (true) {
            val action = player.decisionDirector.cultivationAction(
                availableCards, availableDice, availableBugs
            )
            if (!executeAction(player, action)) {
                break
            }
        }
    }
}
