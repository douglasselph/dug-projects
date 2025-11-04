package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.Die

class SelectDieAnyToReroll(
    private val selectDieToReroll: SelectDieToReroll
) {

    data class BestDie(
        val playerDie: Die? = null,
        val opponentDie: Die? = null
    )

    operator fun invoke(player: Player, target: Player): BestDie {
        // Get the best player die that's below average (value < average)
        val bestPlayerDie = selectDieToReroll(player.diceInHand.dice)?.let { die ->
            if (die.value < die.average) die else null
        }
        
        // Get the best opponent die that's above average (value > average)
        val bestOpponentDie = target.diceInHand.dice
            .filter { it.value > it.average } // Only consider dice above average
            .maxByOrNull { it.value }?.let { maxValueDie ->
                // If there are multiple dice with the same max value, select the one with smallest sides
                target.diceInHand.dice
                    .filter { it.value == maxValueDie.value && it.value > it.average }
                    .minByOrNull { it.sides }
            }
        
        // If no player die available, return opponent die
        if (bestPlayerDie == null) {
            return BestDie(opponentDie = bestOpponentDie)
        }
        
        // If no opponent die available, return player die
        if (bestOpponentDie == null) {
            return BestDie(playerDie = bestPlayerDie)
        }
        
        // Compare the differences from average
        val playerDifference = bestPlayerDie.average - bestPlayerDie.value
        val opponentDifference = bestOpponentDie.value - bestOpponentDie.average
        
        // Select the die with the largest difference from average
        return if (playerDifference >= opponentDifference) {
            BestDie(playerDie = bestPlayerDie)
        } else {
            BestDie(opponentDie = bestOpponentDie)
        }
    }

}
