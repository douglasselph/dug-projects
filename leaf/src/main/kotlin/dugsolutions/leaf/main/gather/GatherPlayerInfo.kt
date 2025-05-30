package dugsolutions.leaf.main.gather

import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.player.Player

class GatherPlayerInfo(
    private val gatherCardInfo: GatherCardInfo
) {

    operator fun invoke(player: Player): PlayerInfo {
        // Dice in hand "D4=1" style
        val handDice = player.diceInHand.dice.map { "D${it.sides}=${it.value}"}

        val handCards = player.cardsInHand.map { gatherCardInfo(it) }

        // Format dice in supply as "4D4 3D6" style
        val supplyDice = player.diceInSupply.dice
            .groupBy { it.sides }
            .map { (sides, dice) -> "${dice.size}D$sides" }
            .sorted()

        // Format dice in compost as "4D4 3D6" style
        val compostDice = player.diceInCompost.dice
            .groupBy { it.sides }
            .map { (sides, dice) -> "${dice.size}D$sides" }
            .sorted()

        val floralArray = player.floralCards.map { gatherCardInfo(it) }

        // Get counts for supply and compost cards
        val supplyCardCount = player.cardsInSupplyCount
        val compostCardCount = player.cardsInCompostCount

        return PlayerInfo(
            name = player.name,
            handCards = handCards,
            handDice = handDice,
            supplyDice = supplyDice,
            floralArray = floralArray,
            supplyCardCount = supplyCardCount,
            compostDice = compostDice,
            compostCardCount = compostCardCount
        )
    }

}
