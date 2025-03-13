package dugsolutions.leaf.game.purchase.cost

import dugsolutions.leaf.game.purchase.domain.Adjusted
import dugsolutions.leaf.game.purchase.domain.Combination
import dugsolutions.leaf.player.Player

class ApplyEffects {

    operator fun invoke(player: Player, combination: Combination) {
        for (adjust in combination.adjusted) {
            when (adjust) {
                is Adjusted.ByAmount -> {
                    player.diceInHand.adjust(adjust.die, adjust.amount)
                    player.effectsList.findAdjustDieRoll(adjust.amount)?.let {
                        player.effectsList.remove(it)
                    }
                }
                is Adjusted.ToMax -> {
                    player.diceInHand.adjustToMax(adjust.die)
                    player.effectsList.findAdjustToMax()?.let {
                        player.effectsList.remove(it)
                    }
                }
            }
        }
    }
}
