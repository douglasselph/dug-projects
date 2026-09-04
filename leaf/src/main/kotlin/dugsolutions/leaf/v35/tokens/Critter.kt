package dugsolutions.leaf.v35.tokens

/**
 * Physical Critter type.
 *
 * Temporary "this round" value changes are player state, not a different
 * physical Critter identity. Use Player.critterValues when a rule needs the
 * Critter's current effective value.
 */
enum class Critter(
    val baseValue: Int
) {
    BEE(2),
    WORM(1)
}
