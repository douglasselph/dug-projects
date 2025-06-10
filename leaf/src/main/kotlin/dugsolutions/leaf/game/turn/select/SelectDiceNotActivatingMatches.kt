package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.CanProcessMatchEffect
import dugsolutions.leaf.random.die.Die

class SelectDiceNotActivatingMatches(
    private val canProcessMatchEffect: CanProcessMatchEffect
) {

    operator fun invoke(player: Player): List<Die> {
        val diceReservedForMatchEffects = diceActivatingMatchEffects(player)
        return removeDiceOnce(player.diceInHand.dice, diceReservedForMatchEffects)
    }

    private fun diceActivatingMatchEffects(player: Player): List<Die> {
        val usingDice = mutableListOf<Die>()
        player.cardsToPlay.forEach() { card ->
                val result = canProcessMatchEffect(card, player)
                result.dieCost?.let { usingDice.add(it) }
            }
        return usingDice.distinct()
    }

    private fun removeDiceOnce(dice: List<Die>, doNotUse: List<Die>): List<Die> {
        val resultList = mutableListOf<Die>()
        val doNotUse2 = doNotUse.toMutableList()
        for (die in dice) {
            if (doNotUse2.contains(die)) {
                doNotUse2.remove(die)
            } else {
                resultList.add(die)
            }
        }
        return resultList
    }
}
