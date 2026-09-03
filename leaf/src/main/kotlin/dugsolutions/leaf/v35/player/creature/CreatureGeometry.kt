package dugsolutions.leaf.v35.player.creature

enum class CreatureSide {
    LEFT,
    RIGHT
}

enum class GraftDirection {
    LEFT,
    RIGHT,
    ABOVE
}

/**
 * Logical grid position only. It is not a UI/pixel coordinate.
 *
 * Plant Core is (0, 0).
 * Player Dice Supply is (0, -1).
 */
data class CreaturePosition(
    val x: Int,
    val y: Int
) {
    fun move(direction: GraftDirection): CreaturePosition =
        when (direction) {
            GraftDirection.LEFT -> copy(x = x - 1)
            GraftDirection.RIGHT -> copy(x = x + 1)
            GraftDirection.ABOVE -> copy(y = y + 1)
        }
}

/**
 * A legal place in which a new Plant card may be grafted.
 *
 * Side records which of the Creature's two growth structures the card belongs
 * to. Position records where it physically sits relative to the Core/Supply.
 */
data class GraftPlacement(
    val side: CreatureSide,
    val position: CreaturePosition
)
