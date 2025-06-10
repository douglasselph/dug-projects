package dugsolutions.leaf.main.domain

import dugsolutions.leaf.grove.domain.MarketStackID

data class GroveInfo(
    val stacks: List<StackInfo> = emptyList(),
    val dice: DiceInfo = DiceInfo(),
    val instruction: String? = null,
    val quantities: String? = null
)

data class StackInfo(
    val stack: MarketStackID,
    val topCard: CardInfo?,
    val numCards: Int
) {

    val name: String
        get() {
            return when (stack) {
                MarketStackID.ROOT_1 -> "Root 1"
                MarketStackID.ROOT_2 -> "Root 2"
                MarketStackID.CANOPY_1 -> "Canopy 1"
                MarketStackID.CANOPY_2 -> "Canopy 2"
                MarketStackID.VINE_1 -> "Vine 1"
                MarketStackID.VINE_2 -> "Vine 2"
                MarketStackID.WILD_1 -> "Wild 1"
                MarketStackID.WILD_2 -> "Wild 2"
                MarketStackID.FLOWER_1 -> "Flower 1"
                MarketStackID.FLOWER_2 -> "Flower 2"
                MarketStackID.FLOWER_3 -> "Flower 3"
            }
        }

    /**
     * 10's - Column 1
     * 20's - Column 2
     * 30's - Column 3
     *
     * The single digit is the row position.
     * So "21" means Column 2, Row 1.
     */
    val order: Int
        get() {
            return when (stack) {
                MarketStackID.ROOT_1 -> 11
                MarketStackID.ROOT_2 -> 12
                MarketStackID.CANOPY_1 -> 13
                MarketStackID.CANOPY_2 -> 14
                MarketStackID.VINE_1 -> 21
                MarketStackID.VINE_2 -> 22
                MarketStackID.WILD_1 -> 23
                MarketStackID.WILD_2 -> 24
                MarketStackID.FLOWER_1 -> 31
                MarketStackID.FLOWER_2 -> 32
                MarketStackID.FLOWER_3 -> 33
            }
        }
}
