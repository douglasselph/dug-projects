package dugsolutions.leaf.v35.tokens

enum class Critter(val value: Int) {
    BEE(2),
    WORM(1),
    BOOSTED_WORM(3),
    BOOSTED_BEE(4);

    val normal: Critter
        get() {
            return when (this) {
                BEE, BOOSTED_BEE -> BEE
                WORM, BOOSTED_WORM -> WORM
            }
        }
}
