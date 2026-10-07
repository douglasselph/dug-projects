package dugsolutions.leaf.simulation.v35.learning.mulch

import dugsolutions.leaf.v35.player.decision.learned.mulch.LearnedMulchTargetWeights
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

data class MulchTargetEvolutionConfig(
    val populationSize:Int=8, val eliteCount:Int=2, val mutationSigma:Double=.25,
    val mutationsPerChild:Int=8, val evolutionSeed:Long=109000L
) { init { require(populationSize>=2); require(eliteCount in 1 until populationSize) } }

data class EvaluatedMulchTargetPolicy(val weights:LearnedMulchTargetWeights,val fitness:Double)

class MulchTargetPolicyEvolution(private val c:MulchTargetEvolutionConfig){
    private val r=Random(c.evolutionSeed)
    fun initialPopulation(seed:LearnedMulchTargetWeights)=listOf(seed)+List(c.populationSize-1){mutate(seed)}
    fun nextPopulation(e:List<EvaluatedMulchTargetPolicy>):List<LearnedMulchTargetWeights>{
        val elites=e.sortedByDescending{it.fitness}.take(c.eliteCount).map{it.weights}
        return buildList{addAll(elites);while(size<c.populationSize)add(mutate(elites[r.nextInt(elites.size)]))}
    }
    private fun mutate(p:LearnedMulchTargetWeights):LearnedMulchTargetWeights{
        val a=p.toDoubleArray();repeat(c.mutationsPerChild){val i=r.nextInt(a.size);a[i]+=gaussian()*c.mutationSigma}
        return LearnedMulchTargetWeights.fromDoubleArray(a,p.provenance)
    }
    private fun gaussian():Double{var u=r.nextDouble();while(u<=0)u=r.nextDouble();return sqrt(-2*ln(u))*cos(2*Math.PI*r.nextDouble())}
}
