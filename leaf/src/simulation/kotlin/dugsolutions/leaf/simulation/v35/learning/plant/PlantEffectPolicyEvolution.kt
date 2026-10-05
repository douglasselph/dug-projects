package dugsolutions.leaf.simulation.v35.learning.plant

import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectWeights
import kotlin.math.sqrt
import kotlin.random.Random

data class EvaluatedPlantEffectPolicy(val weights:LearnedPlantEffectWeights,val fitness:Double)
data class PlantEffectEvolutionConfig(val populationSize:Int=8,val eliteCount:Int=2,val mutationSigma:Double=.25,val mutationsPerChild:Int=8,val evolutionSeed:Long=96000L){init{require(populationSize>=2);require(eliteCount in 1 until populationSize)}}
class PlantEffectPolicyEvolution(private val config:PlantEffectEvolutionConfig){private val r=Random(config.evolutionSeed);fun initialPopulation(seed:LearnedPlantEffectWeights)=listOf(seed)+List(config.populationSize-1){mutate(seed)};fun nextPopulation(e:List<EvaluatedPlantEffectPolicy>):List<LearnedPlantEffectWeights>{val elites=e.sortedByDescending{it.fitness}.take(config.eliteCount).map{it.weights};return buildList{addAll(elites);while(size<config.populationSize)add(mutate(elites[r.nextInt(elites.size)]))}};private fun mutate(p:LearnedPlantEffectWeights):LearnedPlantEffectWeights{val a=p.toDoubleArray();val n=p.namedWeights().toMutableMap();val keys=n.keys.sorted();repeat(config.mutationsPerChild){val i=r.nextInt(a.size+keys.size);val d=gaussian()*config.mutationSigma;if(i<a.size)a[i]+=d else{val k=keys[i-a.size];n[k]=n.getValue(k)+d}};return LearnedPlantEffectWeights.fromDoubleArray(a,p.provenance,n)};private fun gaussian():Double{var u=r.nextDouble();while(u<=0)u=r.nextDouble();return sqrt(-2*kotlin.math.ln(u))*kotlin.math.cos(2*Math.PI*r.nextDouble())}}
