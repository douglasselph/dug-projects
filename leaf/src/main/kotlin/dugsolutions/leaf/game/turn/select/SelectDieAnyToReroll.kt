package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.Die

// TODO: Unit test
class SelectDieAnyToReroll(
    private val selectDieToReroll: SelectDieToReroll
) {

    data class BestDie(
        val playerDie: Die? = null,
        val opponentDie: Die? = null
    )

    operator fun invoke(player: Player, target: Player): BestDie {
        val bestPlayerDie = selectDieToReroll(player.diceInHand.dice)
        val bestOpponentDie = target.diceInHand.dice
            .maxWithOrNull(compareByDescending<Die> { it.value }.thenBy { it.sides })
        if (bestPlayerDie == null) {
            return BestDie(opponentDie = bestOpponentDie)
        } else if (bestOpponentDie == null) {
            return BestDie(playerDie = bestPlayerDie)
        }
        val differencePlayer = bestPlayerDie.sides - bestPlayerDie.value
        if (differencePlayer > bestOpponentDie.value) {
            return BestDie(playerDie = bestPlayerDie)
        }
        return BestDie(opponentDie = bestOpponentDie)
    }

}
