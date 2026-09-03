package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantCards
import dugsolutions.leaf.v35.plant.domain.PlantType

/**
 * Provides the in-memory collection of Plant card definitions used by the game.
 *
 * Cards are keyed by their stable CSV "name" field rather than by generated
 * integer IDs.
 */
class PlantCardManager {

    private var cardsByName: Map<String, PlantCard> = emptyMap()

    fun loadCards(cardRegistry: PlantCardRegistry) {
        loadCards(cardRegistry.getAllCards())
    }

    fun loadCards(incoming: List<PlantCard>) {
        val reset = linkedMapOf<String, PlantCard>()

        incoming.forEach { card ->
            val key = card.name.normalizedName()

            require(key !in reset) {
                "Duplicate Plant card name '${card.name}'"
            }

            reset[key] = card
        }

        cardsByName = reset
    }

    fun getCard(name: String): PlantCard? =
        cardsByName[name.normalizedName()]

    fun getPlantCardsByType(type: PlantType): PlantCards =
        PlantCards(getCardsByType(type))

    fun getCardsByType(type: PlantType): List<PlantCard> =
        cardsByName.values.filter { it.type == type }

    fun getAllCards(): PlantCards =
        PlantCards(cardsByName.values.toList())

    val size: Int
        get() = cardsByName.size

    fun clear() {
        cardsByName = emptyMap()
    }

    private fun String.normalizedName(): String =
        trim().lowercase()
}
