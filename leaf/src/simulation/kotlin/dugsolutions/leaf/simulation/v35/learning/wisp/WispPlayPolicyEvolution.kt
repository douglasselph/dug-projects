package dugsolutions.leaf.simulation.v35.learning.wisp

import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayWeights
import kotlin.math.sqrt
import kotlin.random.Random

data class EvaluatedWispPlayPolicy(val weights:LearnedWispPlayWeights,val fitness:Double)
data class WispPlayEvolutionConfig(val populationSize:Int=8,val eliteCount:Int=2,val mutationSigma:Double=.25,val mutationsPerChild:Int=8,val evolutionSeed:Long=55000L){init{require(populationSize>=2);require(eliteCount in 1 until populationSize)}}
class WispPlayPolicyEvolution(private val config:WispPlayEvolutionConfig){private val r=Random(config.evolutionSeed);fun initialPopulation(seed:LearnedWispPlayWeights)=listOf(seed)+List(config.populationSize-1){mutate(seed)};fun nextPopulation(e:List<EvaluatedWispPlayPolicy>):List<LearnedWispPlayWeights>{val elites=e.sortedByDescending{it.fitness}.take(config.eliteCount).map{it.weights};return buildList{addAll(elites);while(size<config.populationSize)add(mutate(elites[r.nextInt(elites.size)]))}};private fun mutate(p:LearnedWispPlayWeights):LearnedWispPlayWeights{val a=p.toDoubleArray();val n=p.namedWeights().toMutableMap();val keys=n.keys.sorted();repeat(config.mutationsPerChild){val i=r.nextInt(a.size+keys.size);val d=gaussian()*config.mutationSigma;if(i<a.size)a[i]+=d else{val k=keys[i-a.size];n[k]=n.getValue(k)+d}};return LearnedWispPlayWeights.fromDoubleArray(a,p.provenance,n)};private fun gaussian():Double{var u=r.nextDouble();while(u<=0)u=r.nextDouble();return sqrt(-2*kotlin.math.ln(u))*kotlin.math.cos(2*Math.PI*r.nextDouble())}}
