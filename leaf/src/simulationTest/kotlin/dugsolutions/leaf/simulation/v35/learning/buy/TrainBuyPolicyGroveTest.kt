package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentOverride
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrainBuyPolicyGroveTest {
    @Test
    fun `Cultivation Main policy option defaults to Human`() {
        assertEquals("human", TrainOptions.parse(emptyList()).cultivationMainPolicy)
        assertEquals("human", TrainOptions.parse(listOf("--cultivation-main-policy=human")).cultivationMainPolicy)
    }

    @Test
    fun `Battle Support policy option defaults to Human`() {
        assertEquals("human", TrainOptions.parse(emptyList()).battleSupportPolicy)
        assertEquals("human", TrainOptions.parse(listOf("--battle-support-policy=human")).battleSupportPolicy)
        assertFailsWith<IllegalArgumentException> {
            TrainOptions.parse(listOf("--battle-support-policy=learned"))
        }
    }


    private data class Catalog(
        val manager: PlantCardManager,
        val defaultGrove: List<dugsolutions.leaf.v35.plant.domain.PlantCard>,
        val allPlants: List<dugsolutions.leaf.v35.plant.domain.PlantCard>,
    )

    private fun catalog(): Catalog {
        val registry = PlantCardRegistry().apply {
            loadFromCsv(
                CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST),
            )
        }
        val manager = PlantCardManager().apply { loadCards(registry) }
        return Catalog(
            manager = manager,
            defaultGrove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(manager.getCard(it)) },
            allPlants = manager.getAllCards().cards,
        )
    }

    @Test
    fun `training defaults remain FirstGameDefault`() {
        val options = TrainOptions.parse(emptyList())
        assertNull(options.grovePattern)
        assertEquals(81000L, options.groveSeed)

        val c = catalog()
        val groves = resolveTrainingGroves(options.copy(games = 3), c.manager, c.defaultGrove, c.allPlants)
        val expected = GrovePlantCode.encode(c.defaultGrove)
        assertEquals(listOf(expected, expected, expected), groves.map(GrovePlantCode::encode))
    }


    @Test
    fun `training player count defaults to four and accepts two or three`() {
        assertEquals(4, TrainOptions.parse(emptyList()).players)
        assertEquals(2, TrainOptions.parse(listOf("--players", "2")).players)
        assertEquals(3, TrainOptions.parse(listOf("--players=3")).players)
    }

    @Test
    fun `random Grove training is reproducible and respects Plant availability`() {
        val options = TrainOptions.parse(listOf("--games", "12", "--random-grove", "--grove-seed", "81234"))
        val c = catalog()
        val values = PlantExperimentConfig.of(
            "Vine_07_01" to PlantExperimentOverride(available = false),
            "Vine_07_04" to PlantExperimentOverride(available = false),
        )

        val first = resolveTrainingGroves(options, c.manager, c.defaultGrove, c.allPlants, values)
        val second = resolveTrainingGroves(options, c.manager, c.defaultGrove, c.allPlants, values)

        assertEquals(first.map(GrovePlantCode::encode), second.map(GrovePlantCode::encode))
        first.forEach { grove ->
            val names = grove.map { it.name }
            assertTrue("Vine_07_01" !in names)
            assertTrue("Vine_07_04" !in names)
            assertTrue(names.any { it == "Vine_07_02" || it == "Vine_07_03" })
        }
    }

    @Test
    fun `fixed training Grove rejects unavailable explicitly requested card`() {
        val options = TrainOptions.parse(listOf("--games", "1", "--grove", "243114324"))
        val c = catalog()
        val values = PlantExperimentConfig.of(
            "Vine_07_01" to PlantExperimentOverride(available = false),
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            resolveTrainingGroves(options, c.manager, c.defaultGrove, c.allPlants, values)
        }
        assertTrue(failure.message.orEmpty().contains("Vine_07_01"))
    }

    @Test
    fun `training and evaluation share Grove resolution semantics`() {
        val c = catalog()
        val values = PlantExperimentConfig.of(
            "Vine_07_04" to PlantExperimentOverride(available = false),
        )
        val train = TrainOptions.parse(listOf("--games", "4", "--grove", "000100000", "--grove-seed", "84567"))
        val eval = EvalOptions.parse(listOf("--games", "4", "--grove", "000100000", "--grove-seed", "84567"))
        val trainingGroves = resolveTrainingGroves(train, c.manager, c.defaultGrove, c.allPlants, values)

        val evaluationCodes = (0 until 4).map { sample ->
            GrovePlantCode.encode(resolveGroveForSample(eval, sample, c.manager, c.defaultGrove, c.allPlants, values))
        }

        assertEquals(trainingGroves.map(GrovePlantCode::encode), evaluationCodes)
    }
}
