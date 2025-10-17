package dugsolutions.leaf.game.acquire.cost

import dugsolutions.leaf.game.acquire.domain.Adjusted
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.Player

class ApplyEffects {

    operator fun invoke(player: Player, combination: Combination) {
        for (adjust in combination.adjusted) {
            when (adjust) {
                is Adjusted.ByAmount -> {
                    player.diceInHand.adjust(adjust.die, adjust.amount)
                    player.delayedEffectList.findAdjustDieRoll(adjust.amount)?.let {
                        player.delayedEffectList.remove(it)
                    }
                }
                is Adjusted.ToMax -> {
                    player.diceInHand.adjustToMax(adjust.die)
                    player.delayedEffectList.findAdjustToMax()?.let {
                        player.delayedEffectList.remove(it)
                    }
                }
            }
        }
    }
}
