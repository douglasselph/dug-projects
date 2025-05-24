package dugsolutions.leaf.game.turn

import dugsolutions.leaf.game.turn.handle.HandleCardEffect
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.CardsEffectsProcessor

/***
 *  Process card effects that can be done immediately before the acquisition or battle phases.
 */
class PlayerRound(
    private val cardsEffectsProcessor: CardsEffectsProcessor,
    private val handleCardEffect: HandleCardEffect
) {
    operator fun invoke(player: Player, target: Player) {

        player.effectsList.clear()

        var result = HandleCardEffect.EffectResult(
            newCards = player.cardsInHand
        )
        while (result.hasMoreToProcess) {
            // Process new cards and collect effects
            cardsEffectsProcessor(result.newCards, player)
            // Process card effects (which may add result in new cards or dice rolls)
            result = handleCardEffect(player, target)
        }

    }

} 
