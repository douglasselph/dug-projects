package dugsolutions.leaf.game.acquire.credit

import dugsolutions.leaf.game.acquire.domain.Credit
import dugsolutions.leaf.game.acquire.domain.Credits
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.AppliedEffect

class CreditsUseCase {

    operator fun invoke(player: Player): Credits {
        val list = mutableListOf<Credit>()
        for (die in player.diceInHand.dice) {
            list.add(Credit.CredDie(die))
        }
        for (effect in player.delayedEffectList) {
            handleEffect(effect)?.let { list.add(it) }
        }
        if (player.pipModifier > 0) {
            list.add(Credit.CredAddToTotal(player.pipModifier))
        }
        return Credits(list)
    }

    private fun handleEffect(effect: AppliedEffect): Credit? {
        return when (effect) {
            is AppliedEffect.MarketBenefit -> effect.type?.let { Credit.CredReduceCost(it, effect.costReduction) }
            else -> null
        }
    }
}
