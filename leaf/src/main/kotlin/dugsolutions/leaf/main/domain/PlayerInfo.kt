package dugsolutions.leaf.main.domain

data class PlayerInfo(
    val name: String,
    val handCards: List<CardInfo>,
    val handDice: List<String>,
    val supplyDice: List<String>,
    val floralArray: List<CardInfo>,
    val supplyCardCount: Int,
    val compostCardCount: Int,
    val compostDice: List<String>
)
