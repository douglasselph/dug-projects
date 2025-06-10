package dugsolutions.leaf.player.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player

class CardEffectsProcessor(
    private val cardEffectProcessor: CardEffectProcessor,
    private val chronicle: GameChronicle
) {
    suspend operator fun invoke(card: GameCard, player: Player) {
        val cardEffects = cardEffectProcessor(card, player)
        player.delayedEffectList.addAll(cardEffects)
        if (cardEffects.isNotEmpty()) {
            chronicle(Moment.PLAY_CARD(player, card))
        }
    }
} 
