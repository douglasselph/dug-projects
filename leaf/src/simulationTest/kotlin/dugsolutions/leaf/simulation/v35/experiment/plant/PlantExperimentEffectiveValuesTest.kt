package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.simulation.v35.learning.buy.EvalOptions
import dugsolutions.leaf.simulation.v35.learning.buy.TrainOptions
import dugsolutions.leaf.simulation.v35.learning.buy.evaluationGameConfig
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlantExperimentEffectiveValuesTest {

    private val cards by lazy {
        PlantCardRegistry().apply {
            loadFromCsv(
                CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
            )
        }.getAllCards()
    }

    @Test
    fun noCliOptionResolvesCanonicalPlantValues() {
        val eval = EvalOptions.parse(emptyList())
        val train = TrainOptions.parse(emptyList())
        assertNull(eval.plantOverridesPath)
        assertNull(train.plantOverridesPath)

        val resolved = PlantExperimentResearchConfig.resolve(null, cards)
        assertSame(PlantExperimentConfig.EMPTY, resolved.values)
        cards.forEach { card ->
            assertEquals(card.cost, resolved.values.costFor(card))
            assertTrue(resolved.values.isAvailable(card))
        }
    }

    @Test
    fun trainingAndEvaluationUseTheSameOverrideFileSemantics() {
        val path = Files.createTempFile("plant-overrides-shared", ".csv")
        Files.writeString(
            path,
            "card_id,cost,available,scoring\n" +
                "Vine_07_01,11,true,\n" +
                "Vine_07_04,,false,\n"
        )
        try {
            val eval = EvalOptions.parse(listOf("--plant-overrides", path.toString()))
            val train = TrainOptions.parse(listOf("--plant-overrides", path.toString()))
            val evalResolved = PlantExperimentResearchConfig.resolve(eval.plantOverridesPath, cards)
            val trainResolved = PlantExperimentResearchConfig.resolve(train.plantOverridesPath, cards)

            assertEquals(evalResolved.values, trainResolved.values)
            val berry = cards.single { it.name == "Vine_07_01" }
            val yield = cards.single { it.name == "Vine_07_04" }
            assertEquals(11, evalResolved.values.costFor(berry))
            assertEquals(false, evalResolved.values.isAvailable(yield))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun pairedEvaluationConfigsCarryTheSameImmutablePlantExperiment() {
        val values = PlantExperimentConfig.of(
            "Vine_07_01" to PlantExperimentOverride(cost = 11)
        )
        val controlDecisions = List(4) { PlayerDecisionFactory.humanBaseline() }
        val learnedRoleDecisions = List(4) { PlayerDecisionFactory.humanBaseline() }

        val control = evaluationGameConfig(
            emptyList(), controlDecisions, GameRoundSetup.standard(), 101L, 201L, values
        )
        val learned = evaluationGameConfig(
            emptyList(), learnedRoleDecisions, GameRoundSetup.standard(), 101L, 201L, values
        )

        assertSame(values, control.plantValues)
        assertSame(values, learned.plantValues)
        assertEquals(control.seed, learned.seed)
        assertEquals(control.strategySeed, learned.strategySeed)
    }

    @Test
    fun reportUsesCanonicalValuesForResolvedInterventions() {
        val berry = cards.single { it.name == "Vine_07_01" }
        val yield = cards.single { it.name == "Vine_07_04" }
        val values = PlantExperimentConfig.of(
            berry.name to PlantExperimentOverride(cost = 11),
            yield.name to PlantExperimentOverride(
                available = false,
                scoringRule = PlantScoringRule.PerGraftedFlower
            )
        )
        val rendered = PlantExperimentResearchConfig(
            sourcePath = java.nio.file.Paths.get("/tmp/example.csv"),
            values = values
        ).render(cards)

        assertTrue(rendered.contains("Vine_07_01"))
        assertTrue(rendered.contains("cost: ${berry.cost} -> 11"))
        assertTrue(rendered.contains("Vine_07_04"))
        assertTrue(rendered.contains("available: true -> false"))
        assertTrue(rendered.contains("scoring: PER_GRAFTED_VINE -> PER_GRAFTED_FLOWER"))
        assertTrue(rendered.contains("All unspecified Plant properties canonical."))
    }

    @Test
    fun learnedBuyCatalogIncludesExperimentalExactCostFeaturesWithoutChangingCanonicalManifest() {
        val berry = cards.single { it.name == "Vine_07_01" }
        val values = PlantExperimentConfig.of(
            berry.name to PlantExperimentOverride(cost = 8)
        )
        val prepared = LearnedBuyCardCatalog.prepare(
            LearnedBuyWeights.zeros(),
            cards,
            additionalPlantCosts = cards.map(values::costFor)
        )

        assertTrue("ACTION_COST_8" in prepared.namedWeights())
        assertTrue("ACTION_COST_8_STAGE_1" in prepared.namedWeights())
    }
}
