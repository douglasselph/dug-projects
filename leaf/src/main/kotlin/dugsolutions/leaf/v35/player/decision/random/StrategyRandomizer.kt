package dugsolutions.leaf.v35.player.decision.random

import kotlin.random.Random

/**
 * Randomness owned by a player's strategy rather than by the game rules.
 *
 * This deliberately exposes only a bounded integer draw. Human Baseline uses
 * it both for tied-score selection and for explicitly probabilistic human
 * tendencies (for example, whether surplus Critters are spent during Buy).
 * Keeping this as a separate type makes it difficult to accidentally consume
 * the Game's mechanical RNG for a strategy choice.
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
