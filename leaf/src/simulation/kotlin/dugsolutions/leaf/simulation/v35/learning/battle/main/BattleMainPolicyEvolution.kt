package dugsolutions.leaf.simulation.v35.learning.battle.main

import dugsolutions.leaf.v35.player.decision.learned.battle.main.LearnedBattleMainWeights
import kotlin.math.sqrt
import kotlin.random.Random

data class EvaluatedBattleMainPolicy(val weights: LearnedBattleMainWeights, val fitness: Double)

data class BattleMainEvolutionConfig(
    val populationSize: Int = 8,
    val eliteCount: Int = 2,
    val mutationSigma: Double = 0.25,
    val mutationsPerChild: Int = 8,
    val evolutionSeed: Long = 54000L
) {
    init {
        require(populationSize >= 2)
        require(eliteCount in 1 until populationSize)
        require(mutationSigma > 0.0 && mutationSigma.isFinite())
        require(mutationsPerChild >= 1)
    }
}

class BattleMainPolicyEvolution(private val config: BattleMainEvolutionConfig) {
    private val random = Random(config.evolutionSeed)

    fun initialPopulation(seed: LearnedBattleMainWeights): List<LearnedBattleMainWeights> =
        listOf(seed) + List(config.populationSize - 1) { mutate(seed) }

    fun nextPopulation(evaluated: List<EvaluatedBattleMainPolicy>): List<LearnedBattleMainWeights> {
        require(evaluated.size == config.populationSize)
        val elites = evaluated.sortedByDescending { it.fitness }.take(config.eliteCount).map { it.weights }
        return buildList {
            addAll(elites)
            while (size < config.populationSize) add(mutate(elites[random.nextInt(elites.size)]))
        }
    }

    private fun mutate(parent: LearnedBattleMainWeights): LearnedBattleMainWeights {
        val standard = parent.toDoubleArray()
        val named = parent.namedWeights().toMutableMap()
        val namedKeys = named.keys.sorted()
        val total = standard.size + namedKeys.size
        repeat(config.mutationsPerChild) {
            val index = random.nextInt(total)
            val delta = gaussian() * config.mutationSigma
            if (index < standard.size) standard[index] += delta
            else {
                val key = namedKeys[index - standard.size]
                named[key] = named.getValue(key) + delta
            }
        }
        return LearnedBattleMainWeights.fromDoubleArray(standard, parent.provenance, named)
    }

    private fun gaussian(): Double {
        var u1 = random.nextDouble()
        while (u1 <= 0.0) u1 = random.nextDouble()
        val u2 = random.nextDouble()
        return sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
    }
}
