package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.player.Player

class HandleAbsorbDamage(
    private val chronicle: GameChronicle
) {

    /**
     * Deal with the incoming damage that a player has to face.
     * Return the amount of thorn damage the attacker then needs to face in turn.
     */
    operator fun invoke(player: Player): Int {
        // Only handle damage if the player has incoming damage
        if (!player.hasIncomingDamage()) return 0

        // Decide how to absorb damage
        val result = player.decisionDirector.damageAbsorptionDecision() ?: return 0
        var thornDamage = 0
        // Apply decision
        result.cards.forEach { card ->
            player.removeCardFromHand(card.id)
            thornDamage += card.thorn
            chronicle(GameChronicle.Moment.TRASH_CARD(player, card))
        }
        result.floralCards.forEach { card ->
            player.removeCardFromFloralArray(card.id)
            chronicle(GameChronicle.Moment.TRASH_CARD(player, card, floralArray = true))
        }
        result.dice.forEach { die ->
            player.removeDieFromHand(die)
            chronicle(GameChronicle.Moment.TRASH_DIE(player, die))
        }

        // If player has no more cards in hand, clear the FloralArray
        if (player.cardsInHand.isEmpty()) {
            // Chronicle each floral card before clearing
            player.floralCards.forEach { card ->
                chronicle(GameChronicle.Moment.TRASH_CARD(player, card, floralArray = true))
            }
            player.clearFloralCards()
        }
        return thornDamage
    }
} 
