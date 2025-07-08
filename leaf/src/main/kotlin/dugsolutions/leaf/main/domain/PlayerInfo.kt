package dugsolutions.leaf.main.domain

import dugsolutions.leaf.cards.domain.GameCard

data class PlayerInfo(
    val name: String,
    val infoLine: String,
    val handCards: List<CardInfo>,
    val handDice: DiceInfo,
    val supplyDice: DiceInfo,
    val floralArray: List<CardInfo>,
    val nutrients: Int,
    val supplyCardCount: Int,
    val discardCardCount: Int,
    val discardDice: DiceInfo,
    val decidingPlayer: Boolean = false,
    val humanControlled: Boolean = false
) {

    fun copyForItemSelect(): PlayerInfo {
        return copy(
            handCards = handCards.map { it.copy(highlight = HighlightInfo.SELECTABLE) },
            handDice = handDice.copyForItemSelect(),
            floralArray = floralArray.map { it.copy(highlight = HighlightInfo.SELECTABLE) }
        )
    }

    fun copyForFlowerSelect(): PlayerInfo {
        return copy(
            floralArray = floralArray.map { it.copy(highlight = HighlightInfo.SELECTABLE) }
        )
    }

    fun copyForCardSelect(card: GameCard): PlayerInfo {
        return copy(
            handCards = handCards.map {
                if (it.name == card.name) {
                    it.copy(highlight = HighlightInfo.SELECTABLE)
                } else it
            },
            floralArray = floralArray.map {
                if (it.name == card.name) {
                    it.copy(highlight = HighlightInfo.SELECTABLE)
                } else it
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerInfo

        if (name != other.name) return false
        if (handCards != other.handCards) return false
        if (handDice != other.handDice) return false
        if (supplyDice != other.supplyDice) return false
        if (floralArray != other.floralArray) return false
        if (supplyCardCount != other.supplyCardCount) return false
        if (discardCardCount != other.discardCardCount) return false
        if (discardDice != other.discardDice) return false
        if (decidingPlayer != other.decidingPlayer) return false
        if (humanControlled != other.humanControlled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + handCards.hashCode()
        result = 31 * result + handDice.hashCode()
        result = 31 * result + supplyDice.hashCode()
        result = 31 * result + floralArray.hashCode()
        result = 31 * result + supplyCardCount
        result = 31 * result + discardCardCount
        result = 31 * result + discardDice.hashCode()
        result = 31 * result + decidingPlayer.hashCode()
        result = 31 * result + humanControlled.hashCode()
        return result
    }

}
