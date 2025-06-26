package dugsolutions.leaf.main.gather

import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.local.DecidingPlayer
import dugsolutions.leaf.player.Player

class GatherPlayerInfo(
    private val gatherCardInfo: GatherCardInfo,
    private val gatherDiceInfo: GatherDiceInfo,
    private val decidingPlayer: DecidingPlayer
) {

    operator fun invoke(player: Player): PlayerInfo {
        // Dice in hand "D4=1" style
        val handDice = gatherDiceInfo(player.diceInHand, values = true)

        val handCards = player.cardsInHand.mapIndexed { index, item -> gatherCardInfo(index, item) }

        // Format dice in supply as "4D4 3D6" style
        val supplyDice = gatherDiceInfo(player.diceInSupply, values = false)

        // Format dice in compost as "4D4 3D6" style
        val discardPatch = gatherDiceInfo(player.diceInDiscard, values = false)

        val floralArray = player.floralCards.sortedBy { it.name }.mapIndexed { index, item -> gatherCardInfo(index, item) }

        // Get counts for supply and compost cards
        val supplyCardCount = player.cardsInSupplyCount
        val bedCardCount = player.cardsInDiscardCount

        return PlayerInfo(
            name = player.name,
            infoLine = infoLineFrom(player),
            nutrients = player.nutrients,
            handCards = handCards,
            handDice = handDice,
            supplyDice = supplyDice,
            floralArray = floralArray,
            supplyCardCount = supplyCardCount,
            discardDice = discardPatch,
            discardCardCount = bedCardCount,
            decidingPlayer = (player.name == decidingPlayer.player?.name)
        )
    }

    private fun infoLineFrom(player: Player): String {
        return totalFrom(player) + handDiceFrom(player) + supplyLine(player) + discardLine(player) + floralLine(player)
    }

    private fun totalFrom(player: Player): String {
        val countDice = player.allDice.size
        val countCards = player.allCardsInDeck.size
        return "Total Cards/Dice: $countCards/$countDice"
    }

    private fun handDiceFrom(player: Player): String {
        val diceValues = player.diceInHand.sort().map { it.toValue() }
        val countCards = player.cardsInHand.size
        return "\nHand Cards: $countCards, Dice: $diceValues"
    }

    private fun supplyLine(player: Player): String {
        val diceLine = player.diceInSupply.toString()
        val countCards = player.cardsInSupplyCount
        return "\nSupply Cards: $countCards, Dice: $diceLine"
    }

    private fun discardLine(player: Player): String {
        val diceLine = player.diceInDiscard.toString()
        val countCards = player.cardsInDiscard.size
        return "\nDiscard Cards: $countCards, Dice: $diceLine"
    }

    private fun floralLine(player: Player): String {
        return if (player.floralCards.isNotEmpty()) {
            "\nFloral Cards: " + player.floralCards.joinToString(",") { it.name }
        } else ""
    }

}
