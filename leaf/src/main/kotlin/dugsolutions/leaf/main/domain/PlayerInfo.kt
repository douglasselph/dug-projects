package dugsolutions.leaf.main.domain

data class PlayerInfo(
    val name: String,
    val handCards: List<CardInfo>,
    val handDice: DiceInfo,
    val supplyDice: DiceInfo,
    val floralArray: List<CardInfo>,
    val supplyCardCount: Int,
    val compostCardCount: Int,
    val compostDice: DiceInfo,
    val showDrawCount: Boolean = false
)
