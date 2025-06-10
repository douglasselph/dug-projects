package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.local.EffectBattleScore

/**
 * For simplicity just select the card with the highest non-flower battle score effects.
 * Only allow usage of opponent's root or canopy cards.
 * For safety do not copy cards with USE_OPPONENT_CARD otherwise there will be an infinite loop.
 */
class EffectUseOpponentCard(
    private val effectBattleScore: EffectBattleScore,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, target: Player) {
        target.cardsInHand.filter {
            it.primaryEffect != CardEffect.USE_OPPONENT_CARD &&
                    (it.type == FlourishType.CANOPY || it.type == FlourishType.ROOT)
        }.maxByOrNull { evaluateScore(it) }?.let { selected ->
            player.cardsToPlay.add(selected)
            chronicle(Moment.USE_OPPONENT_CARD(player, selected))
        }
    }

    private fun evaluateScore(card: GameCard): Int {
        return effectBattleScore(card.primaryEffect, card.primaryValue)
    }

}
