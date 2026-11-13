package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImplementedBattleFixtureTest {
    @Test
    fun `all nine fixture plants resolve through the normalized PlantCardManager catalog`() {
        val root = CardDataFiles.dataDirectory()
        val registry = PlantCardRegistry().apply {
            loadFromCsv(
                CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root),
                CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root)
            )
        }
        val manager = PlantCardManager().apply { loadCards(registry) }

        val plants = ImplementedBattleFixture.plants(manager)
        assertEquals(9, plants.size)
        assertEquals(9, plants.map { it.type to it.cost }.toSet().size)
        assertEquals(
            listOf("Flower_11_01", "Flower_14_01", "Flower_17_04"),
            plants.filter { it.name.startsWith("Flower_") }.map { it.name }
        )
        plants.forEach { assertNotNull(manager.getCard(it.name)) }
    }
}
