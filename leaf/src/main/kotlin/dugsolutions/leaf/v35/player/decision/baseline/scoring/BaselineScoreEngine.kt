package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer

/**
 * Selects the highest-scoring Human Baseline candidate.
 *
 * The strategy RNG is consumed only when two or more candidates share the
 * highest total. Mechanical game randomness is never accepted by this class.
 */
class BaselineScoreEngine(
    private val randomizer: StrategyRandomizer = StrategyRandomizer.create()
) {
    fun <T> choose(
        choices: List<ScoredChoice<T>>
    ): ScoredChoice<T> {
        require(choices.isNotEmpty()) {
            "Cannot choose from an empty scored-choice list"
        }

        val highest = choices.maxOf { it.score.total }
        val tied = choices.filter { it.score.total == highest }

        return if (tied.size == 1) {
            tied.single()
        } else {
            tied[randomizer.nextInt(tied.size)]
        }
    }

    fun <T> chooseValue(
        choices: List<ScoredChoice<T>>
    ): T = choose(choices).choice
}
