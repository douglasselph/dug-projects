package dugsolutions.leaf.main.domain

import dugsolutions.leaf.grove.domain.MarketStackID

data class GroveInfo(
    val stacks: List<StackInfo>,
    val selectText: String? = null
)

data class StackInfo(
    val stack: MarketStackID,
    val topCard: CardInfo?,
    val numCards: Int,
    val highlight: Boolean = false
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
                MarketStackID.JOINT_RCV -> "Wild"
                MarketStackID.FLOWER_1 -> "Flower 1"
                MarketStackID.FLOWER_2 -> "Flower 2"
                MarketStackID.FLOWER_3 -> "Flower 3"
            }
        }
}
