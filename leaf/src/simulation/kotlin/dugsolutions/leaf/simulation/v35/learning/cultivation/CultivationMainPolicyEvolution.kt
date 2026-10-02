package dugsolutions.leaf.simulation.v35.learning.cultivation

import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import kotlin.math.sqrt
import kotlin.random.Random

data class EvaluatedCultivationMainPolicy(
    val weights: LearnedCultivationMainWeights,
    val fitness: Double
)

data class CultivationMainEvolutionConfig(
    val populationSize: Int = 8,
    val eliteCount: Int = 2,
    val mutationSigma: Double = 0.25,
    val mutationsPerChild: Int = 8,
    val evolutionSeed: Long = 52000L
) {
    init {
        require(populationSize >= 2)
        require(eliteCount in 1 until populationSize)
        require(mutationSigma > 0.0 && mutationSigma.isFinite())
        require(mutationsPerChild >= 1)
    }
}

/** Small deterministic evolutionary optimizer for the transparent linear Main-action policy. */
class CultivationMainPolicyEvolution(
    private val config: CultivationMainEvolutionConfig
) {
    private val random = Random(config.evolutionSeed)

    fun initialPopulation(seed: LearnedCultivationMainWeights): List<LearnedCultivationMainWeights> =
        listOf(seed) + List(config.populationSize - 1) { mutate(seed) }

    fun nextPopulation(evaluated: List<EvaluatedCultivationMainPolicy>): List<LearnedCultivationMainWeights> {
        require(evaluated.size == config.populationSize)
        require(evaluated.all { it.fitness.isFinite() })
        val elites = evaluated.sortedByDescending { it.fitness }
            .take(config.eliteCount)
            .map { it.weights }
        return buildList {
            addAll(elites)
            while (size < config.populationSize) add(mutate(elites[random.nextInt(elites.size)]))
        }
    }

    fun mutate(parent: LearnedCultivationMainWeights): LearnedCultivationMainWeights {
        val standard = parent.toDoubleArray()
        val named = parent.namedWeights().toMutableMap()
        val namedKeys = named.keys.sorted()
        val total = standard.size + namedKeys.size
        require(total > 0)
        repeat(config.mutationsPerChild) {
            val index = random.nextInt(total)
            val delta = gaussian() * config.mutationSigma
            if (index < standard.size) {
                standard[index] += delta
            } else {
                val key = namedKeys[index - standard.size]
                named[key] = named.getValue(key) + delta
            }
        }
        return LearnedCultivationMainWeights.fromDoubleArray(
            standard,
            parent.provenance,
            named
        )
    }

    private fun gaussian(): Double {
        var u1 = random.nextDouble()
        while (u1 <= 0.0) u1 = random.nextDouble()
        val u2 = random.nextDouble()
        return sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
    }
}
