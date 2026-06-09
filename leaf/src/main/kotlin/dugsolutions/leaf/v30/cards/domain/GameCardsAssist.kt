package dugsolutions.leaf.v30.cards.domain

fun List<GameCard>.getByType(type: CardType): List<GameCard> {
    return this.filter { it.type == type }
}

fun List<GameCard>.getFlourishTypes(): List<CardType> {
    return this.map { it.type }.distinct()
}
