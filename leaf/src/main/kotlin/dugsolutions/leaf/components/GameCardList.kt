package dugsolutions.leaf.components

fun List<GameCard>.getByType(type: FlourishType): List<GameCard> {
    return this.filter { it.type == type }
}

fun List<GameCard>.getFlourishTypes(): List<FlourishType> {
    return this.map { it.type }.distinct()
}