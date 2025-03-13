package dugsolutions.leaf.main.info

data class PlayerInfo(
    val handCards: List<String>,
    val handDice: List<String>,
    val supplyDice: List<String>,
    val supplyCardCount: Int,
    val compostDiceCount: Int,
    val compostCardCount: Int
)