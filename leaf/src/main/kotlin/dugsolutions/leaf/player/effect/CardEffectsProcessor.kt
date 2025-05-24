package dugsolutions.leaf.player.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player

class CardEffectsProcessor(
    private val cardEffectProcessor: CardEffectProcessor,
    private val chronicle: GameChronicle
) {
    operator fun invoke(card: GameCard, player: Player) {
        val cardEffects = cardEffectProcessor(card, player)
        player.effectsList.addAll(cardEffects)
        if (cardEffects.isNotEmpty()) {
            chronicle(GameChronicle.Moment.PLAY_CARD(player, card))
        }
    }
} 
