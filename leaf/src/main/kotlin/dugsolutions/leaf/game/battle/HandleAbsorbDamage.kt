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
        if (player.getExtendedHandItems().isEmpty()) return 0

        // Decide how to absorb damage
        var result = player.decisionDirector.damageAbsorptionDecision()
        val damageToAbsorb = player.incomingDamage
        if (result.allEmpty) {
            result = DecisionDamageAbsorption.Result(
                cards = player.cardsInHand.toList(),
                dice = player.diceInHand.dice.toList(),
                floralCards = player.floralCards.toList(),
                damageToAbsorb = damageToAbsorb
            )
        }
        var thornDamage = 0
        // Apply decision
        result.cards.forEach { card ->
            player.removeCardFromHand(card.id)
            player.incomingDamage -= card.resilience
            player.nutrients += card.nutrient
            thornDamage += card.thorn
            chronicle(Moment.TRASH_CARD(player, card))
        }
        result.floralCards.forEach { card ->
            player.removeCardFromBuddingStack(card.id)
            player.incomingDamage -= card.resilience
            player.nutrients += card.nutrient
            thornDamage += card.thorn
            chronicle(Moment.TRASH_CARD(player, card, floralArray = true))
        }
        result.dice.forEach { die ->
            player.removeDieFromHand(die)
            player.incomingDamage -= die.sides
            chronicle(Moment.TRASH_DIE(player, die))
        }
        if (player.incomingDamage < 0) {
            player.incomingDamage = 0
        } else if (player.incomingDamage > 0) {
            if (player.getExtendedHandItems().isNotEmpty()) {
                thornDamage += invoke(player)
            }
        }
        return thornDamage
    }
} 
