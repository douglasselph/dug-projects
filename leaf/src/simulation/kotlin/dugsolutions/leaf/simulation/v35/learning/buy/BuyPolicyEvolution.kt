package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.v35.player.decision.learned.buy.BuyFeature
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import kotlin.math.sqrt
import kotlin.random.Random

/** One evaluated immutable policy. Fitness is intentionally supplied by the game experiment. */
data class EvaluatedBuyPolicy(val weights: LearnedBuyWeights, val fitness: Double)

data class BuyEvolutionConfig(
    val populationSize: Int = 8,
    val eliteCount: Int = 2,
    val mutationSigma: Double = 0.25,
    val mutationsPerChild: Int = 6,
    val evolutionSeed: Long = 51000L
) {
    init {
        require(populationSize >= 2)
        require(eliteCount in 1 until populationSize)
        require(mutationSigma > 0.0 && mutationSigma.isFinite())
        require(mutationsPerChild in 1..BuyFeature.entries.size)
    }
}

/**
 * Small deterministic evolutionary optimizer for the transparent linear Buy policy.
 * Elites survive unchanged; every other child is a mutation of an elite.
 */
class BuyPolicyEvolution(private val config: BuyEvolutionConfig) {
    private val random = Random(config.evolutionSeed)

    fun initialPopulation(seed: LearnedBuyWeights): List<LearnedBuyWeights> =
        listOf(seed) + List(config.populationSize - 1) { mutate(seed) }

    fun nextPopulation(evaluated: List<EvaluatedBuyPolicy>): List<LearnedBuyWeights> {
        require(evaluated.size == config.populationSize)
        require(evaluated.all { it.fitness.isFinite() })
        val elites = evaluated.sortedByDescending { it.fitness }.take(config.eliteCount).map { it.weights }
        return buildList {
            addAll(elites)
            while (size < config.populationSize) add(mutate(elites[random.nextInt(elites.size)]))
        }
    }

    fun mutate(parent: LearnedBuyWeights): LearnedBuyWeights {
        val values = parent.toDoubleArray()
        repeat(config.mutationsPerChild) {
            val index = random.nextInt(values.size)
            values[index] += gaussian() * config.mutationSigma
        }
        return LearnedBuyWeights.fromDoubleArray(values, parent.provenance)
    }

    // Box-Muller, driven only by the dedicated deterministic evolution RNG.
    private fun gaussian(): Double {
        var u1 = random.nextDouble()
        while (u1 <= 0.0) u1 = random.nextDouble()
        val u2 = random.nextDouble()
        return sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
    }
}
