package dugsolutions.leaf.simulation.v35.learning.cultivation.support

import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportWeights
import kotlin.math.sqrt
import kotlin.random.Random

data class EvaluatedCultivationSupportPolicy(val weights: LearnedCultivationSupportWeights, val fitness: Double)

data class CultivationSupportEvolutionConfig(
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

class CultivationSupportPolicyEvolution(private val config: CultivationSupportEvolutionConfig) {
    private val random = Random(config.evolutionSeed)
    fun initialPopulation(seed: LearnedCultivationSupportWeights) = listOf(seed) + List(config.populationSize - 1) { mutate(seed) }
    fun nextPopulation(evaluated: List<EvaluatedCultivationSupportPolicy>): List<LearnedCultivationSupportWeights> {
        require(evaluated.size == config.populationSize)
        val elites=evaluated.sortedByDescending { it.fitness }.take(config.eliteCount).map { it.weights }
        return buildList { addAll(elites); while(size<config.populationSize) add(mutate(elites[random.nextInt(elites.size)])) }
    }
    fun mutate(parent: LearnedCultivationSupportWeights): LearnedCultivationSupportWeights {
        val standard=parent.toDoubleArray(); val named=parent.namedWeights().toMutableMap(); val keys=named.keys.sorted(); val total=standard.size+keys.size
        repeat(config.mutationsPerChild) {
            val idx=random.nextInt(total); val delta=gaussian()*config.mutationSigma
            if(idx<standard.size) standard[idx]+=delta else { val k=keys[idx-standard.size]; named[k]=named.getValue(k)+delta }
        }
        return LearnedCultivationSupportWeights.fromDoubleArray(standard,parent.provenance,named)
    }
    private fun gaussian():Double { var u1=random.nextDouble(); while(u1<=0.0)u1=random.nextDouble(); val u2=random.nextDouble(); return sqrt(-2.0*kotlin.math.ln(u1))*kotlin.math.cos(2.0*Math.PI*u2) }
}
