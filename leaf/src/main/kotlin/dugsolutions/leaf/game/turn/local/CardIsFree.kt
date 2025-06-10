package dugsolutions.leaf.game.turn.local

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.AppliedEffect

class CardIsFree {

    operator fun invoke(card: GameCard, player: Player): Boolean {
        for (effect in player.delayedEffectList) {
            when (effect) {
                is AppliedEffect.MarketBenefit -> {
                    if (effect.type == card.type) {
                        if (effect.isFree) {
                            // Card is free, no need to check further
                            return true
                        }
                    }
                }
                else -> { /* Ignore other effect types */ }
            }
        }
        return false
    }
}
