package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.random.Randomizer

/**
 * Resolves the nine Plant slots required by a Grove.
 *
 * Explicit cards are slot overrides. Every unfilled slot is selected from the
 * loaded Plant catalog using the game's mechanical Randomizer. Research Plant
 * type/cost overrides define effective slot membership before selection.
 */
class GrovePlantResolver(
    private val plantCardManager: PlantCardManager
) {
    fun resolve(
        overrides: List<PlantCard>,
        randomizer: Randomizer,
        plantValues: PlantValueResolver = PlantValueResolver.CANONICAL
    ): List<PlantCard> {
        val bySlot = linkedMapOf<PlantSlot, PlantCard>()

        overrides.forEach { suppliedCard ->
            val catalogCard = plantCardManager.getCard(suppliedCard.name)
            requireNotNull(catalogCard) { "Unknown Plant card override: ${suppliedCard.name}" }
            val effectiveCard = plantValues.effectiveCardFor(catalogCard)
            val slot = PlantSlot.from(effectiveCard)
            require(slot in REQUIRED_SLOTS) {
                "Plant card '${catalogCard.name}' does not belong to a Grove slot after active Plant overrides: " +
                    "${effectiveCard.type} ${effectiveCard.cost}"
            }
            require(slot !in bySlot) {
                "Multiple Plant cards supplied for Grove slot ${slot.label}: " +
                    "'${bySlot.getValue(slot).name}' and '${catalogCard.name}'"
            }
            require(suppliedCard.type == effectiveCard.type && suppliedCard.cost == effectiveCard.cost) {
                "Plant card override '${suppliedCard.name}' does not match its active effective definition: " +
                    "supplied=${suppliedCard.type} ${suppliedCard.cost}, effective=${effectiveCard.type} ${effectiveCard.cost}"
            }
            require(plantValues.isAvailable(catalogCard)) {
                "Plant card '${catalogCard.name}' is unavailable in the active Plant experiment and cannot be fixed into Grove slot ${slot.label}"
            }
            bySlot[slot] = effectiveCard
        }

        return REQUIRED_SLOTS.map { slot ->
            bySlot[slot] ?: randomizer.randomOrNull(
                plantCardManager.getAllCards().cards
                    .filter(plantValues::isAvailable)
                    .filter { plantValues.typeFor(it) == slot.type && plantValues.costFor(it) == slot.cost }
            )?.let(plantValues::effectiveCardFor) ?: error(
                "No Plant cards available for Grove slot ${slot.label} after applying Plant experiment type/cost/availability overrides"
            )
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
