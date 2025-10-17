package dugsolutions.leaf.common.domain

enum class Butterfly(
    val effect: Effect,
    val value: Int
) {

    YELLOW(Effect.REROLL_TAKE_BETTER, 1),
    GREEN(Effect.ADD_TO_DIE, 1)

}
