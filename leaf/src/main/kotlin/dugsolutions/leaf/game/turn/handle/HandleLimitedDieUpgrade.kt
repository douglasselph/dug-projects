package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.MissingDieException
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player

class HandleLimitedDieUpgrade(
    private val dieFactory: DieFactory,
    private val grove: Grove
) {
    operator fun invoke(player: Player, only: List<DieSides>, discardAfterUse: Boolean): Die? {
        val dice = player.diceInHand

        // Find the highest-sided die from the allowed sides
        val dieToUpgrade = dice.dice
            .filter { die -> only.any { allowed -> allowed.value == die.sides } }
            .maxByOrNull { it.sides } ?: return null

        // Determine the next die size in sequence
        val nextDieSize = when (dieToUpgrade.sides) {
            4 -> DieSides.D6
            6 -> DieSides.D8
            8 -> DieSides.D10
            10 -> DieSides.D12
            12 -> DieSides.D20
            else -> return null // Invalid die size
        }

        // Remove the die being upgraded
        if (!player.removeDieFromHand(dieToUpgrade)) {
            throw MissingDieException("Could not locate the die we are about to upgrade: $dieToUpgrade")
        }

        // Acquire the new die
        val newDie = dieFactory(nextDieSize).roll()
        grove.removeDie(newDie)
        grove.addDie(dieToUpgrade)

        if (discardAfterUse) {
            player.addDieToCompost(newDie)
        } else {
            player.addDieToHand(newDie)
        }
        return newDie
    }
}
