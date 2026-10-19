package dugsolutions.leaf.v14.game.turn.handle

import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.effect.NutrientReward

// TODO: Add logic for BOT that if running out of dice, to activate it.
class HandleCompostRecovery(
    private val nutrientReward: NutrientReward
) {

    operator fun invoke(player: Player) {
        if (player.nutrients >= 10) {
            nutrientReward(player)
        }
    }
}
