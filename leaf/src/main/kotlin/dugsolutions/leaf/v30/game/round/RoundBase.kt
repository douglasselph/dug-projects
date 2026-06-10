package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

abstract class RoundBase(
    protected val table: Table,
    val card: RoundCard
) {

    private companion object {
        const val DICE_PER_PLAYER_PER_ROUND = 3
        const val ROLL_GAIN_CRITTER = 1
        const val ROLL_GAIN_WISP = 2
    }

    open fun run() {
    }

    fun drawDice() {
        table.players.forEach { player ->
            player.discardHandDice()
            repeat(DICE_PER_PLAYER_PER_ROUND) {
                player.drawDiceWithRefresh()
            }
        }
    }

    fun rollDice() {
        table.players.forEach { player ->
            player.rollDice()
        }
    }

    fun resolveRewards() {
        table.players.forEach { player ->
            player.diceHand.dice.forEach { die ->
                when (die.value) {
                    ROLL_GAIN_CRITTER -> gainCritter(player)
                    ROLL_GAIN_WISP -> table.grove.drawWispCard()?.let { player.addWispCard(it) }
                }
            }
        }
    }

    private fun gainCritter(player: Player) {
        val critter = chooseCritter(player)
        if (table.grove.remove(critter)) {
            player.addCritter(critter)
        }
    }

    private fun chooseCritter(player: Player): Critter {
        val bees = player.critters.count { it == Critter.BEE }
        val worms = player.critters.count { it == Critter.WORM }

        // Decision point: replace this automatic choice once player decisions are modeled.
        return if (bees <= worms) Critter.BEE else Critter.WORM
    }

}
