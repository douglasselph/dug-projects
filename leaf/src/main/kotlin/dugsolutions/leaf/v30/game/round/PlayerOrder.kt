package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.Randomizer

class PlayerOrder(
    private val randomizer: Randomizer = Randomizer.create()
) {
    operator fun invoke(players: List<Player>): List<Player> {
        if (players.isEmpty()) return emptyList()
        val firstPlayerIndex = determineFirstPlayerIndex(players)
        return players.drop(firstPlayerIndex) + players.take(firstPlayerIndex)
    }

    private fun determineFirstPlayerIndex(players: List<Player>): Int {
        val handValues = players.map { player ->
            player.diceHand.dice.map { it.value }.sortedDescending()
        }
        val maxHandSize = handValues.maxOf { it.size }

        for (index in 0 until maxHandSize) {
            val valuesAtIndex = handValues.map { values -> values.getOrElse(index) { 0 } }
            val maxValue = valuesAtIndex.max()
            val winners = valuesAtIndex.withIndex()
                .filter { it.value == maxValue }
                .map { it.index }
            if (winners.size == 1) return winners.first()
        }

        return rollD20ForFirstPlayer(players)
    }

    private fun rollD20ForFirstPlayer(players: List<Player>): Int {
        val rolls = players.map { randomizer.nextInt(1, D20_MAX + 1) }
        val highestRoll = rolls.max()
        return rolls.indexOf(highestRoll)
    }

    private companion object {
        const val D20_MAX = 20
    }
}