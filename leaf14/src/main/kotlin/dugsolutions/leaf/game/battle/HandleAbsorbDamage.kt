package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption

class HandleAbsorbDamage(
    private val chronicle: GameChronicle
) {

    /**
     * Deal with the incoming damage that a player has to face.
     * Return the amount of thorn damage the attacker then needs to face in turn.
     */
    suspend operator fun invoke(player: Player): Int {
        // Only handle damage if the player has incoming damage
        if (player.incomingDamage <= 0) return 0

        // Decide how to absorb damage
        var result = player.decisionDirector.damageAbsorptionDecision()
        // Apply decision
        val thornDamage = result.thorn
        result.handCards.forEach { card ->
            player.removeCardFromHand(card.id)
            player.incomingDamage -= card.resilience
            chronicle(Moment.TRASH_CARD(player, card))
        }
        result.creatureCards.forEach { card ->
            player.removeCardFromCreature(card.id)
            player.incomingDamage -= card.resilience
            chronicle(Moment.TRASH_CARD(player, card))
        }
        result.handDice.forEach { die ->
            player.removeDieFromHand(die)
            player.incomingDamage -= die.sides
            chronicle(Moment.TRASH_DIE(player, die))
        }
        if (player.incomingDamage < 0) {
            player.incomingDamage = 0
        } else if (player.incomingDamage > 0) {
            // Game is lost
            TODO("chronicle event: player has lost")
        }
        return thornDamage
    }
} 
