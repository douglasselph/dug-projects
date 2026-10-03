package dugsolutions.leaf.simulation.v35.learning.battle

import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights
import kotlin.math.sqrt
import kotlin.random.Random

data class EvaluatedBattleSupportPolicy(val weights: LearnedBattleSupportWeights, val fitness: Double)

data class BattleSupportEvolutionConfig(
    val populationSize: Int = 8,
    val eliteCount: Int = 2,
    val mutationSigma: Double = 0.25,
    val mutationsPerChild: Int = 8,
    val evolutionSeed: Long = 53000L
) {
    init { require(populationSize >= 2); require(eliteCount in 1 until populationSize); require(mutationSigma > 0); require(mutationsPerChild > 0) }
}

class BattleSupportPolicyEvolution(private val config: BattleSupportEvolutionConfig) {
    private val random = Random(config.evolutionSeed)
    fun initialPopulation(seed: LearnedBattleSupportWeights) = listOf(seed) + List(config.populationSize - 1) { mutate(seed) }
    fun nextPopulation(evaluated: List<EvaluatedBattleSupportPolicy>): List<LearnedBattleSupportWeights> {
        val elites = evaluated.sortedByDescending { it.fitness }.take(config.eliteCount).map { it.weights }
        return buildList { addAll(elites); while (size < config.populationSize) add(mutate(elites[random.nextInt(elites.size)])) }
    }
    fun mutate(parent: LearnedBattleSupportWeights): LearnedBattleSupportWeights {
        val standard = parent.toDoubleArray(); val named = parent.namedWeights().toMutableMap(); val keys = named.keys.sorted(); val total = standard.size + keys.size
        repeat(config.mutationsPerChild) {
            val i = random.nextInt(total); val d = gaussian() * config.mutationSigma
            if (i < standard.size) standard[i] += d else named[keys[i-standard.size]] = named.getValue(keys[i-standard.size]) + d
        }
        return LearnedBattleSupportWeights.fromDoubleArray(standard, parent.provenance, named)
    }
    private fun gaussian(): Double { var u1 = random.nextDouble(); while (u1 <= 0.0) u1 = random.nextDouble(); val u2 = random.nextDouble(); return sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2 * Math.PI * u2) }
}
