package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.game.turn.local.EvaluateSimpleCost
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.game.acquire.cost.ApplyCost
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player

class HandleDieUpgrade(
    private val evaluateSimpleCost: EvaluateSimpleCost,
    private val applyCost: ApplyCost,
    private val dieFactory: DieFactory,
    private val grove: Grove
) {

    companion object {
        private const val UPGRADE_TO_D20_COST = 20
    }

    operator fun invoke(player: Player, discardAfterUse: Boolean): Die? {
        // If has pips and a D12 in play, then choose that.
        // Otherwise choose lowest DIE sides.
        // Note: when spending pips, a die must be discarded. The entire die must be chosen to be
        //  discarded even though only part of was used.
        var dieToUpgrade = findDieToUpgrade(player) ?: return null
        // Determine the new die to create
        var newDie = findNewDie(dieToUpgrade) ?: return null

        // If upgrading to D20, we need to pay 8 pips
        if (newDie.sides == 20) {
            evaluateSimpleCost(player, UPGRADE_TO_D20_COST)?.let { combination ->
                applyCost(player, combination)
            } ?: run {
                dieToUpgrade = findSimpleDieToUpgrade(player) ?: return null
                newDie = findNewDie(dieToUpgrade) ?: return null
            }
        }
        // Remove the die being upgraded
        player.removeDieFromHand(dieToUpgrade)
        grove.addDie(dieToUpgrade)

        // Add the new die
        player.addDieToHand(newDie.roll())
        grove.removeDie(newDie)

        if (discardAfterUse) {
            player.discard(newDie)
        }
        return newDie
    }

    private fun findDieToUpgrade(player: Player): Die? {
        val dice = player.diceInHand
        val pips = player.pipTotal

        // Find the die to upgrade
        return when {
            // If has 8+ pips and a D12, upgrade that
            pips >= 8 && dice.dice.any { it.sides == 12 } ->
                dice.dice.first { it.sides == 12 }
            // Otherwise find the highest sided die that's not a D20
            else -> dice.dice.filter { it.sides < 20 }
                .maxByOrNull { it.sides }
        }
    }

    private fun findNewDie(dieToUpgrade: Die): Die? {
        return when (dieToUpgrade.sides) {
            4 -> dieFactory(DieSides.D6)
            6 -> dieFactory(DieSides.D8)
            8 -> dieFactory(DieSides.D10)
            10 -> dieFactory(DieSides.D12)
            12 -> dieFactory(DieSides.D20)
            else -> return null // Invalid die size
        }
    }

    private fun findSimpleDieToUpgrade(player: Player): Die? {
        val dice = player.diceInHand
        return dice.dice.filter { it.sides < 12 }.maxByOrNull { it.sides }

    }

}
