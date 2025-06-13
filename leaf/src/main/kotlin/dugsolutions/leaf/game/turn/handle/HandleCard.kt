package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.decisions.local.ShouldAskTrashEffect
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.player.effect.CanProcessMatchEffect
import dugsolutions.leaf.player.effect.FlowerCardMatchValue
import dugsolutions.leaf.player.effect.ShouldProcessMatchEffect


class HandleCard(
    private val handleCardEffect: HandleCardEffect,
    private val canProcessMatchEffect: CanProcessMatchEffect,
    private val shouldProcessMatchEffect: ShouldProcessMatchEffect,
    private val shouldAskTrashEffect: ShouldAskTrashEffect,
    private val flowerCardMatchValue: FlowerCardMatchValue,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(player: Player, target: Player, card: GameCard) {
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
                handleCardEffect(player, target, card.matchEffect, card.matchValue + flowerCardMatchValue(player, card))
            }
        }
        // See if we should trash this card right now.
        card.trashEffect?.let {
            when (val result = shouldAskTrashEffect(player, card)) {
                DecisionShouldProcessTrashEffect.Result.TRASH -> {
                    handleCardEffect(player, target, card.trashEffect, card.trashValue)
                    player.removeCardFromHand(card.id)
                    chronicle(Moment.TRASH_FOR_EFFECT(player, card, result))
                }

                DecisionShouldProcessTrashEffect.Result.TRASH_IF_NEEDED -> {
                    player.delayedEffectList.add(AppliedEffect.TrashIfNeeded(card))
                    chronicle(Moment.TRASH_FOR_EFFECT(player, card, result))
                }

                else -> {}
            }
        }
    }

} 
