package dugsolutions.leaf.game.turn

import dugsolutions.leaf.game.turn.handle.HandleDrawEffect
import dugsolutions.leaf.game.turn.handle.HandleLocalCardEffect
import dugsolutions.leaf.game.turn.handle.HandleOpponentEffects
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.CardEffectsProcessor

class PlayerRound(
    private val cardEffectsProcessor: CardEffectsProcessor,
    private val handleDrawEffect: HandleDrawEffect,
    private val handleLocalCardEffect: HandleLocalCardEffect,
    private val handleOpponentEffects: HandleOpponentEffects,
) {
    operator fun invoke(player: Player, target: Player?) {

        player.effectsList.clear()

        // TODO: What really should happen is a selection of which card to play
        //  and the to play that card. Each player plays one card in turn was the original idea.
        //  This idea is pretty complicated though and not necessary for the integration tests
        //  I have planned.

        // 1. Process all cards and collect effects
        for (card in player.cardsToPlay) {
            cardEffectsProcessor(card, player)
        }
        player.cardsToPlay.clear()

        // 2. Process draw effects (which may add new effects)
        handleDrawEffect(player)

        // 3. Process local effects
        handleLocalCardEffect(player)

        // 4. Process opponent effects
        target?.let { handleOpponentEffects(player, target) }
    }

} 
