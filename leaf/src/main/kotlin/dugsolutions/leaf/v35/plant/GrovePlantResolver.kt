package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.random.Randomizer

/**
 * Resolves the nine Plant slots required by a Grove.
 *
 * Explicit cards are slot overrides. Every unfilled slot is selected from the
 * loaded Plant catalog using the game's mechanical Randomizer.
 */
class GrovePlantResolver(
    private val plantCardManager: PlantCardManager
) {
    fun resolve(
        overrides: List<PlantCard>,
        randomizer: Randomizer
    ): List<PlantCard> {
        val bySlot = linkedMapOf<PlantSlot, PlantCard>()

        overrides.forEach { card ->
            val slot = PlantSlot.from(card)
            require(slot in REQUIRED_SLOTS) {
                "Plant card '${card.name}' does not belong to a Grove slot: ${card.type} ${card.cost}"
            }
            require(slot !in bySlot) {
                "Multiple Plant cards supplied for Grove slot ${slot.label}: " +
                    "'${bySlot.getValue(slot).name}' and '${card.name}'"
            }
            val catalogCard = plantCardManager.getCard(card.name)
            requireNotNull(catalogCard) { "Unknown Plant card override: ${card.name}" }
            require(catalogCard.type == card.type && catalogCard.cost == card.cost) {
                "Plant card override '${card.name}' does not match the loaded catalog definition"
            }
            bySlot[slot] = catalogCard
        }

        return REQUIRED_SLOTS.map { slot ->
            bySlot[slot] ?: randomizer.randomOrNull(
                plantCardManager.getCardsByType(slot.type)
                    .filter { it.cost == slot.cost }
            ) ?: error("No Plant cards available for Grove slot ${slot.label}")
        }
    }

    private data class PlantSlot(val type: PlantType, val cost: Int) {
        val label: String
            get() = "${type.name} $cost"

        companion object {
            fun from(card: PlantCard): PlantSlot = PlantSlot(card.type, card.cost)
        }
    }

    private companion object {
        val REQUIRED_SLOTS = listOf(
            PlantSlot(PlantType.ROOT, 5),
            PlantSlot(PlantType.ROOT, 7),
            PlantSlot(PlantType.ROOT, 9),
            PlantSlot(PlantType.VINE, 7),
            PlantSlot(PlantType.VINE, 9),
            PlantSlot(PlantType.VINE, 11),
            PlantSlot(PlantType.FLOWER, 11),
            PlantSlot(PlantType.FLOWER, 14),
            PlantSlot(PlantType.FLOWER, 17)
        )
    }
}
