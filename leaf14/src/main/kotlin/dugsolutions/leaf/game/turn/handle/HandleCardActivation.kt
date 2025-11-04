package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.CanProcessMatchEffect
import dugsolutions.leaf.player.effect.ShouldProcessMatchEffect


class HandleCardActivation(
    private val handleCardEffect: HandleCardEffect,
    private val canProcessMatchEffect: CanProcessMatchEffect,
    private val shouldProcessMatchEffect: ShouldProcessMatchEffect,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(player: Player, card: GameCard) {
        // Process primary effect
        if (card.primaryEffect != null) {
            handleCardEffect(player, target, card.primaryEffect, card.primaryValue)
        }
        // Process match effect if applicable
        if (card.matchEffect != null) {
            val result = canProcessMatchEffect(card, player)
            if (result.possible && shouldProcessMatchEffect(card)) {
                result.dieCost?.let {
                    chronicle(Moment.DISCARD_DIE(player, result.dieCost))
                    player.discard(result.dieCost)
                }
                handleCardEffect(player, target, card.matchEffect, card.matchValue)
            }
        }
    }

} 
