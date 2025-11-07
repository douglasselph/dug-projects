package dugsolutions.leaf.game.battle.domain

fun PlayerValues.fill(count: Int): PlayerValues {
    val currentSize = values.size
    if (currentSize >= count) {
        // Already has enough elements, return copy as-is
        return PlayerValues(player, values)
    }
    
    // Need to pad with empty DieBoosted instances
    val paddingNeeded = count - currentSize
    val filledValues = values + List(paddingNeeded) { DieBoosted.empty() }
    
    return PlayerValues(player, filledValues)
}
