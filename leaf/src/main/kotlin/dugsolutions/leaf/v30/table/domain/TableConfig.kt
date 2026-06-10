package dugsolutions.leaf.v30.table.domain

import dugsolutions.leaf.v30.cards.domain.GameCards

data class TableConfig(
    val cards: GameCards,
    val numPlayers: Int,
    val numBattle: Int,
    val numCultivation: Int
) {
    constructor(
        cards: GameCards,
        numPlayers: Int,
        gameLength: GameLength
    ) : this(
        cards = cards,
        numPlayers = numPlayers,
        numBattle = gameLength.numBattle,
        numCultivation = gameLength.numCultivation
    )
}
