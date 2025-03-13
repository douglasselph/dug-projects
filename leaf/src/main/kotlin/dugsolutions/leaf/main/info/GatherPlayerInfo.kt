package dugsolutions.leaf.main.info

import dugsolutions.leaf.player.Player

class GatherPlayerInfo {

    operator fun invoke(player: Player): PlayerInfo {
        // Format dice in hand as "4D4 3D6" style
        val handDice = player.diceInHand.dice
            .groupBy { it.sides }
            .map { (sides, dice) -> "${dice.size}D$sides" }
            .sorted()

        // Format cards in hand by name only
        val handCards = player.cardsInHand
            .map { it.name }
            .sorted()

        // Format supply dice
        val supplyDice = player.diceInSupply.dice
            .groupBy { it.sides }
            .map { (sides, dice) -> "${dice.size}D$sides" }
            .sorted()

        // Get counts for supply cards, compost dice, and compost cards
        val supplyCardCount = player.cardsInSupplyCount
        val compostDiceCount = player.diceInCompost.size
        val compostCardCount = player.cardsInCompostCount

        return PlayerInfo(
            handCards = handCards,
            handDice = handDice,
            supplyDice = supplyDice,
            supplyCardCount = supplyCardCount,
            compostDiceCount = compostDiceCount,
            compostCardCount = compostCardCount
        )
    }

}