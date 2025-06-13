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

        val handCards = player.cardsInHand.mapIndexed { index, item -> gatherCardInfo(index, item) }

        // Format dice in supply as "4D4 3D6" style
        val supplyDice = gatherDiceInfo(player.diceInSupply, values = false)

        // Format dice in compost as "4D4 3D6" style
        val dormantBed = gatherDiceInfo(player.diceInBed, values = false)

        val buddingStack = player.floralCards.mapIndexed { index, item -> gatherCardInfo(index, item) }

        // Get counts for supply and compost cards
        val supplyCardCount = player.cardsInSupplyCount
        val bedCardCount = player.cardsInBedCount

        return PlayerInfo(
            name = player.name,
            infoLine = infoLineFrom(player),
            nutrients = player.nutrients,
            handCards = handCards,
            handDice = handDice,
            supplyDice = supplyDice,
            buddingStack = buddingStack,
            supplyCardCount = supplyCardCount,
            bedDice = dormantBed,
            bedCardCount = bedCardCount
        )
    }

    private fun infoLineFrom(player: Player): String {
        val score = player.score.toString()
        val countDice = player.allDice.size
        val countCards = player.allCardsInDeck.size
        return "Score: $score  Cards/Dice=$countCards/$countDice"
    }

}
