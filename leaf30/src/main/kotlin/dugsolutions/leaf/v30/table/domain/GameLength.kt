package dugsolutions.leaf.v30.table.domain

enum class GameLength(
    val numBattle: Int,
    val numCultivation: Int
) {
    SHORT(numBattle = 3, numCultivation = 3),
    LONG(numBattle = 6, numCultivation = 6)
}
