package dugsolutions.leaf.main.local

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DiceInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.domain.SelectedItems

class SelectItem {

    // region public

    fun handCard(playerInfo: PlayerInfo, cardInfo: CardInfo): PlayerInfo = with(playerInfo) {
        return hasCard(handCards, cardInfo)?.let { selectedCardInfo ->
            copy(
                handCards = handCards.map { card ->
                    if (selectedCardInfo == card)
                        card.copy(highlight = HighlightInfo.SELECTED)
                    else
                        card
                },
            )
        } ?: playerInfo
    }

    fun floralCard(playerInfo: PlayerInfo, cardInfo: CardInfo): PlayerInfo = with(playerInfo) {
        return hasCard(floralArray, cardInfo)?.let { selectedCardInfo ->
            copy(
                floralArray = floralArray.map { card ->
                    if (selectedCardInfo == card)
                        card.copy(highlight = HighlightInfo.SELECTED)
                    else
                        card
                },
            )
        } ?: playerInfo
    }

    fun die(playerInfo: PlayerInfo, dieInfo: DieInfo): PlayerInfo {
        if (dieInfo.index >= playerInfo.handDice.values.size || dieInfo.index < 0) {
            return playerInfo
        }
        return playerInfo.copy(
            handDice = DiceInfo(
                values = playerInfo.handDice.values.mapIndexed { index, die ->
                    if (index == dieInfo.index)
                        dieInfo.copy(highlight = HighlightInfo.SELECTED)
                    else
                        die
                }
            )
        )
    }


    // endregion public

    private fun hasCard(list: List<CardInfo>, item: CardInfo): CardInfo? {
        if (item.index >= list.size || item.index < 0) {
            return null
        }
        if (list[item.index] == item) {
            return list[item.index]
        }
        return null
    }


}
