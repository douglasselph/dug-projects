package dugsolutions.leaf.player.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.domain.AppliedEffect

/**
 * Main processor for all effects in the game.
 * This class coordinates between different effect types and their application to players.
 */
class CardEffectProcessor(
    private val canProcessMatchEffect: CanProcessMatchEffect,
    private val shouldProcessMatchEffect: ShouldProcessMatchEffect,
    private val appliedEffectUseCase: AppliedEffectUseCase,
    private val flowerCardMatchValue: FlowerCardMatchValue,
    private val chronicle: GameChronicle
) {

    private val effects = mutableListOf<AppliedEffect>()

    /**
     * Process a card effect and return the list of applied effects
     */
    suspend operator fun invoke(
        card: GameCard,
        player: Player
    ): List<AppliedEffect> {
        effects.clear()

        // Process primary effect
        if (card.primaryEffect != null) {
            processEffect(player, card.primaryEffect, card.primaryValue)
        }
        // Process match effect if applicable
        if (card.matchEffect != null) {
            val result = canProcessMatchEffect(card, player)
            if (result.possible && shouldProcessMatchEffect(card)) {
                result.dieCost?.let {
                    chronicle(Moment.DISCARD_DIE(player, result.dieCost))
                    player.discard(result.dieCost)
                }
                processEffect(player, card.matchEffect, card.matchValue + flowerCardMatchValue(player, card))
            }
        }
        // See if we should trash this card right now.
        card.trashEffect?.let {
            when (val result = player.decisionDirector.shouldProcessTrashEffect(card)) {
                DecisionShouldProcessTrashEffect.Result.TRASH -> {
                    processEffect(player, card.trashEffect, card.trashValue)
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
        return effects
    }

    /**
     * Process a single effect and return the resulting applied effect
     */
    private fun processEffect(player: Player, effect: CardEffect, value: Int) {
        appliedEffectUseCase(player, effect, value)?.let { effects.add(it) }
    }

} 
