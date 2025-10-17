package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

class EffectDiscard(
    private val costScore: CostScore,
    private val chronicle: GameChronicle
) {

    enum class DiscardWhich {
        CARDS,
        DICE,
        BOTH
    }

    operator fun invoke(which: DiscardWhich, target: Player) {
        when (which) {
            DiscardWhich.CARDS -> {
                val cardToDiscard = target.cardsInHand.minByOrNull { costScore(it.cost) }
                if (cardToDiscard != null) {
                    target.discard(cardToDiscard.id)
                    chronicle(Moment.DISCARD_CARD(target, cardToDiscard))
                }
            }

            DiscardWhich.DICE -> {
                val dieToDiscard = target.diceInHand.dice.minByOrNull { it.value }
                if (dieToDiscard != null) {
                    target.discard(dieToDiscard)
                    chronicle(Moment.DISCARD_DIE(target, dieToDiscard))
                }
            }

            DiscardWhich.BOTH -> {
                // If any die rolled a 1 or 2, choose one of them (the lowest).
                // Otherwise just choose the card with the lowest cost.
                val lowDice = target.diceInHand.dice.filter { it.value <= 2 }
                if (lowDice.isNotEmpty()) {
                    val dieToDiscard = lowDice.minByOrNull { it.value }
                    if (dieToDiscard != null) {
                        target.discard(dieToDiscard)
                        chronicle(Moment.DISCARD_DIE(target, dieToDiscard))
                    }
                } else {
                    val cardToDiscard = target.cardsInHand.minByOrNull { costScore(it.cost) }
                    if (cardToDiscard != null) {
                        target.discard(cardToDiscard.id)
                        chronicle(Moment.DISCARD_CARD(target, cardToDiscard))
                    }
                }
            }
        }
    }
}
