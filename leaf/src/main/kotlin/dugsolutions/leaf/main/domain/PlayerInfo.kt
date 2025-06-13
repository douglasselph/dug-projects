package dugsolutions.leaf.main.domain

import dugsolutions.leaf.cards.domain.GameCard

data class PlayerInfo(
    val name: String,
    val infoLine: String,
    val handCards: List<CardInfo>,
    val handDice: DiceInfo,
    val supplyDice: DiceInfo,
    val buddingStack: List<CardInfo>,
    val nutrients: Int,
    val supplyCardCount: Int,
    val bedCardCount: Int,
    val bedDice: DiceInfo,
    val showDrawCount: Boolean = false
) {

    fun copyForItemSelect(): PlayerInfo {
        return copy(
            handCards = handCards.map { it.copy(highlight = HighlightInfo.SELECTABLE) },
            handDice = handDice.copyForItemSelect(),
            buddingStack = buddingStack.map { it.copy(highlight = HighlightInfo.SELECTABLE) }
        )
    }

    fun copyForFlowerSelect(): PlayerInfo {
        return copy(
            buddingStack = buddingStack.map { it.copy(highlight = HighlightInfo.SELECTABLE) }
        )
    }

    fun copyForCardSelect(card: GameCard): PlayerInfo {
        return copy(
            handCards = handCards.map {
                if (it.name == card.name) {
                    it.copy(highlight = HighlightInfo.SELECTABLE)
                } else it
            },
            buddingStack = buddingStack.map {
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
        if (buddingStack != other.buddingStack) return false
        if (supplyCardCount != other.supplyCardCount) return false
        if (bedCardCount != other.bedCardCount) return false
        if (bedDice != other.bedDice) return false
        if (showDrawCount != other.showDrawCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + handCards.hashCode()
        result = 31 * result + handDice.hashCode()
        result = 31 * result + supplyDice.hashCode()
        result = 31 * result + buddingStack.hashCode()
        result = 31 * result + supplyCardCount
        result = 31 * result + bedCardCount
        result = 31 * result + bedDice.hashCode()
        result = 31 * result + showDrawCount.hashCode()
        return result
    }

}
