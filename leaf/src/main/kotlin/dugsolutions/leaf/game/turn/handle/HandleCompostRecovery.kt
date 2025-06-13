package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.NutrientReward

// TODO: Unit test
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
