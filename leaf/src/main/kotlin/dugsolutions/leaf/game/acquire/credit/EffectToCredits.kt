package dugsolutions.leaf.game.acquire.credit

import dugsolutions.leaf.game.acquire.domain.Credit
import dugsolutions.leaf.game.acquire.domain.Credits
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.AppliedEffect

class EffectToCredits {

    operator fun invoke(player: Player): Credits {
        val list = mutableListOf<Credit>()
        for (die in player.diceInHand.dice) {
            list.add(Credit.CredDie(die))
        }
        for (effect in player.effectsList) {
            handleEffect(effect)?.let { list.add(it) }
        }
        return Credits(list)
    }

    private fun handleEffect(effect: AppliedEffect): Credit? {
        return when (effect) {
            is AppliedEffect.AdjustDieRoll -> Credit.CredAdjustDie(effect.adjustment)
            is AppliedEffect.AdjustDieToMax -> Credit.CredSetToMax
            is AppliedEffect.AddToTotal -> Credit.CredAddToTotal(effect.amount)
            is AppliedEffect.RerollDie -> Credit.CredRerollDie
            is AppliedEffect.MarketBenefit -> effect.type?.let { Credit.CredReduceCost(it, effect.costReduction) }
            else -> null
        }
    }
}
