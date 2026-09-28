package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.v35.player.decision.learned.buy.BuyFeature
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuyPolicyEvolutionTest {
    @Test fun `initial population preserves seed and mutates children`() {
        val seed = LearnedBuyWeights.zeros()
        val evolution = BuyPolicyEvolution(BuyEvolutionConfig(populationSize=4, eliteCount=1, mutationSigma=.5, mutationsPerChild=3, evolutionSeed=7))
        val population = evolution.initialPopulation(seed)
        assertContentEquals(seed.toDoubleArray(), population.first().toDoubleArray())
        assertTrue(population.drop(1).all { child -> child.toDoubleArray().indices.any { child.toDoubleArray()[it] != 0.0 } })
    }

    @Test fun `next generation preserves best elite exactly`() {
        val zero = LearnedBuyWeights.zeros()
        val best = zero.with(BuyFeature.ACTION_D12, 3.0)
        val evolution = BuyPolicyEvolution(BuyEvolutionConfig(populationSize=3, eliteCount=1, mutationSigma=.2, mutationsPerChild=2, evolutionSeed=9))
        val next = evolution.nextPopulation(listOf(
            EvaluatedBuyPolicy(zero, .20), EvaluatedBuyPolicy(best, .40), EvaluatedBuyPolicy(zero.with(BuyFeature.ACTION_D4, 1.0), .10)
        ))
        assertContentEquals(best.toDoubleArray(), next.first().toDoubleArray())
    }

    @Test fun `same evolution seed produces same mutations`() {
        val seed = LearnedBuyWeights.zeros()
        val config = BuyEvolutionConfig(populationSize=5, eliteCount=2, mutationSigma=.3, mutationsPerChild=4, evolutionSeed=1234)
        val a = BuyPolicyEvolution(config).initialPopulation(seed)
        val b = BuyPolicyEvolution(config).initialPopulation(seed)
        a.zip(b).forEach { (x,y) -> assertContentEquals(x.toDoubleArray(), y.toDoubleArray()) }
        assertEquals(5, a.size)
    }
}
