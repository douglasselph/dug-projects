package dugsolutions.leaf.v35.player.decision.random

import kotlin.random.Random

/**
 * Randomness owned by a player's strategy rather than by the game rules.
 *
 * This deliberately exposes only the operation Human Baseline currently
 * needs: choosing one index from a tied set of equally scored candidates.
 * Keeping this as a separate type makes it difficult to accidentally consume
 * the Game's mechanical RNG while breaking a strategy tie.
 */
fun interface StrategyRandomizer {
    fun nextInt(until: Int): Int

    companion object {
        fun create(seed: Long? = null): StrategyRandomizer =
            DefaultStrategyRandomizer(seed)
    }
}

private class DefaultStrategyRandomizer(
    seed: Long?
) : StrategyRandomizer {
    private val random: Random =
        Random(seed ?: Random.Default.nextLong())

    override fun nextInt(until: Int): Int {
        require(until > 0) {
            "Strategy random upper bound must be positive: $until"
        }
        return random.nextInt(until)
    }
}
