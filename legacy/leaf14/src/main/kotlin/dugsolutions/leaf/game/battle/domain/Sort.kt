package dugsolutions.leaf.game.battle.domain

fun PlayerValues.sort(): PlayerValues {
    val sortedValues = values.sortedWith(
        compareByDescending<DieBoosted> { it.dieValue.value }
            .thenBy { it.dieValue.sides }
    )
    return PlayerValues(player, sortedValues)
}
