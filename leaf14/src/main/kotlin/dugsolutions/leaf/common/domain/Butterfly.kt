package dugsolutions.leaf.common.domain

enum class Butterfly(
    val effect: GameEffect,
    val value: Int
) {

    YELLOW(GameEffect.REROLL_TAKE_BETTER, 1),
    GREEN(GameEffect.ADD_TO_DIE, 1),
    PURPLE(GameEffect.ADD_TO_DIE, 1)

}
