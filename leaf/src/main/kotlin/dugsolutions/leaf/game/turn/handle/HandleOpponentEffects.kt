package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.AppliedEffect

class HandleOpponentEffects(
    private val selectDieToAdjust: SelectDieToAdjust,
    private val costScore: CostScore,
    private val chronicle: GameChronicle
) {

    /**
     * Common opponent affects for both phases of the game.
     */
    operator fun invoke(player: Player, target: Player) {
        for (effect in player.effectsList.copy()) {
            if (handleEffect(effect, target)) {
                player.effectsList.remove(effect)
                target.wasHit = true
            }
        }
    }

    private fun handleEffect(effect: AppliedEffect, target: Player): Boolean {
        return when (effect) {

            is AppliedEffect.AdjustDieRoll -> {
                if (!effect.canTargetPlayer) {
                    return false
                }
                // From diceInHand choose the first die found that where the die can fully be applied.
                selectDieToAdjust(target.diceInHand, -effect.adjustment)?.let { die ->
                    target.diceInHand.adjust(die, -effect.adjustment)
                    chronicle(GameChronicle.Moment.ADJUST_DIE(target, -effect.adjustment))
                }
                true
            }
            
            is AppliedEffect.Discard -> {
                if (effect.cardsOnly) {
                    // Choose card with the lowest cost.
                    val cardToDiscard = target.cardsInHand.minByOrNull { costScore(it.cost) }
                    if (cardToDiscard != null) {
                        target.discard(cardToDiscard.id)
                        chronicle(GameChronicle.Moment.DISCARD_CARD(target, cardToDiscard))
                    }
                } else if (effect.diceOnly) {
                    // Choose the die with the lowest rolled value
                    val dieToDiscard = target.diceInHand.dice.minByOrNull { it.value }
                    if (dieToDiscard != null) {
                        target.discard(dieToDiscard)
                        chronicle(GameChronicle.Moment.DISCARD_DIE(target, dieToDiscard))
                    }
                } else {
                    // If any die rolled a 1 or 2, choose one of them (the lowest).
                    // Otherwise just choose the card with the lowest cost.
                    val lowDice = target.diceInHand.dice.filter { it.value <= 2 }
                    if (lowDice.isNotEmpty()) {
                        val dieToDiscard = lowDice.minByOrNull { it.value }
                        if (dieToDiscard != null) {
                            target.discard(dieToDiscard)
                            chronicle(GameChronicle.Moment.DISCARD_DIE(target, dieToDiscard))
                        }
                    } else {
                        val cardToDiscard = target.cardsInHand.minByOrNull { costScore(it.cost) }
                        if (cardToDiscard != null) {
                            target.discard(cardToDiscard.id)
                            chronicle(GameChronicle.Moment.DISCARD_CARD(target, cardToDiscard))
                        }
                    }
                }
                true
            }
            else -> false
        }
    }

} 
