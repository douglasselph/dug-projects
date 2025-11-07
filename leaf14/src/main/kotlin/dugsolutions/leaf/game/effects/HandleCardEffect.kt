package dugsolutions.leaf.game.effects

import dugsolutions.leaf.common.domain.ActivateCard
import dugsolutions.leaf.game.effects.match.CanProcessMatchEffect
import dugsolutions.leaf.game.effects.match.PayMatchEffect
import dugsolutions.leaf.player.Player


class HandleCardEffect(
    private val handleGameEffect: HandleGameEffect,
    private val canProcessMatchEffect: CanProcessMatchEffect,
    private val payMatchEffect: PayMatchEffect
) {

    operator fun invoke(player: Player, target: Player?, activate: ActivateCard) {
        val card = activate.card
        // Process primary effect
        if (card.primaryEffect != null) {
            handleGameEffect(player, target, card.primaryEffect, card.primaryValue)
        }
        // Process match effect if applicable
        if (card.matchEffect != null) {
            if (canProcessMatchEffect(player, card.matchWith, activate.token)) {
                if (payMatchEffect(player, activate.token)) {
                    handleGameEffect(player, target, card.matchEffect, card.matchValue)
                }
            }
        }
    }

} 
