package dugsolutions.leaf.v35.player.decision.learned.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.plant.HumanPlantEffectPolicy
import dugsolutions.leaf.v35.player.decision.plant.PlantEffectDecisionRecord
import dugsolutions.leaf.v35.player.decision.plant.PlantEffectDecisionTrace
import dugsolutions.leaf.v35.player.decision.plant.PlantEffectSource
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LearnedPlantEffectPolicyTest {
    private val first = object : EffectStrategy {
        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice = request.legalChoices.first()
    }

    @Test
    fun `human plant effect policy preserves exact existing effect strategy`() {
        val p = HumanPlantEffectPolicy()
        val strategy = p.strategyFor(PlantEffectSource("Root_05_01", GameEffect.RAISE_DIE_PLUS_4), first)
        val a = EffectDieChoice(0, 6, 1)
        val b = EffectDieChoice(1, 6, 6)
        assertEquals(a, strategy.chooseDie(ChooseEffectDieRequest(GameEffect.RAISE_DIE_PLUS_4, listOf(a,b), DecisionContext.EMPTY)))
    }

    @Test
    fun `learned die targeting selects only legal candidate and records human disagreement`() {
        val raw = DoubleArray(PlantEffectFeature.entries.size)
        raw[PlantEffectFeature.TARGET_DIE_VALUE.ordinal] = 10.0
        val weights = LearnedPlantEffectWeights.fromDoubleArray(raw)
        val traces = mutableListOf<PlantEffectDecisionRecord>()
        val policy = LearnedPlantEffectPolicy(weights)
        val strategy = policy.strategyFor(
            PlantEffectSource("Root_05_01", GameEffect.RAISE_DIE_PLUS_4, PlantEffectDecisionTrace { traces += it }),
            first
        )
        val low = EffectDieChoice(0, 8, 1)
        val high = EffectDieChoice(1, 8, 7)
        val chosen = strategy.chooseDie(ChooseEffectDieRequest(GameEffect.RAISE_DIE_PLUS_4, listOf(low, high), DecisionContext.EMPTY))
        assertEquals(high, chosen)
        assertTrue(chosen in listOf(low, high))
        assertEquals(1, traces.size)
        assertEquals("DIE", traces.single().decisionKind)
        assertTrue(traces.single().selectedChoiceId != traces.single().referenceChoiceId)
    }

    @Test
    fun `weights persist and reload`() {
        val values = DoubleArray(PlantEffectFeature.entries.size)
        values[PlantEffectFeature.BIAS.ordinal] = 1.25
        val original = LearnedPlantEffectWeights.fromDoubleArray(values).withProvenance(
            LearnedPlantEffectProvenance(trainingStatus = "trained", roundPattern = "3/2/2")
        )
        val path = Files.createTempFile("plant-effect", ".weights")
        try {
            original.save(path)
            val loaded = LearnedPlantEffectWeights.load(path)
            assertEquals(1.25, loaded[PlantEffectFeature.BIAS])
            assertEquals("trained", loaded.provenance.trainingStatus)
            assertEquals("3/2/2", loaded.provenance.roundPattern)
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
