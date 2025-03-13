package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.player.Player

class HandleAbsorbDamage(
    private val chronicle: GameChronicle
) {
    operator fun invoke(player: Player) {
        // Only handle damage if the player has incoming damage
        if (!player.hasIncomingDamage()) return

        // Decide how to absorb damage
        val result = player.decisionDirector.damageAbsorptionDecision() ?: return

        // Apply decision
        result.cardIds.forEach { cardId ->
            player.removeCardFromHand(cardId)
            chronicle(GameChronicle.Moment.TRASH_CARD(player, cardId))
        }
        result.dice.forEach { die ->
            player.removeDieFromHand(die)
            chronicle(GameChronicle.Moment.TRASH_DIE(player, die))
        }
    }
} 
