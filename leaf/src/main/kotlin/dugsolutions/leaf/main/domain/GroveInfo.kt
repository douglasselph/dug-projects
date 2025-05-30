package dugsolutions.leaf.main.domain

data class GroveInfo (
    val stacks: List<StackInfo>
)

data class StackInfo(
    val name: String,
    val topCard: CardInfo?,
    val numCards: Int
)
