package dugsolutions.leaf.main.gather

import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.player.Player

class GatherPlayerInfo(
    private val gatherCardInfo: GatherCardInfo,
    private val gatherDiceInfo: GatherDiceInfo
) {

    operator fun invoke(player: Player): PlayerInfo {
        // Dice in hand "D4=1" style
        val handDice = gatherDiceInfo(player.diceInHand, values = true)

        val handCards = player.cardsInHand.map { gatherCardInfo(it) }

        // Format dice in supply as "4D4 3D6" style
        val supplyDice = gatherDiceInfo(player.diceInSupply, values = false)

        // Format dice in compost as "4D4 3D6" style
        val compostDice = gatherDiceInfo(player.diceInCompost, values = false)

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
