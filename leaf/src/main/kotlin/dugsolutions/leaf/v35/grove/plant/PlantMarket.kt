package dugsolutions.leaf.v35.grove.plant

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType

/**
 * The nine selected Plant-card stacks in one game's Grove.
 *
 * A v35 Grove contains exactly:
 * - 3 Root stacks
 * - 3 Vine stacks
 * - 3 Flower stacks
 *
 * Stack identity is the selected PlantCard's stable name. No separate
 * GroveCardStackID is required.
 */
class PlantMarket(
    selectedCards: List<PlantCard>
) : Iterable<PlantStack> {

    private val plantStacks: List<PlantStack>

    init {
        validate(selectedCards)
        plantStacks = selectedCards
            .map(::PlantStack)
    }

    override fun iterator(): Iterator<PlantStack> =
        stacks.iterator()

    val stacks: List<PlantStack>
        get() = plantStacks.toList()

    val availableStacks: List<PlantStack>
        get() = plantStacks.filter { it.isNotEmpty }

    val size: Int
        get() = plantStacks.size

    fun stackFor(card: PlantCard): PlantStack? =
        stackFor(card.name)

    fun stackFor(cardName: String): PlantStack? =
        plantStacks.firstOrNull {
            it.card.name.trim().equals(
                cardName.trim(),
                ignoreCase = true
            )
        }

    /**
     * Takes one copy of the selected PlantCard from its matching Grove stack.
     */
    fun take(card: PlantCard): PlantCard? =
        stackFor(card)?.take()

    /**
     * Returns one copy to the matching selected Grove stack.
     *
     * Unknown/unselected card definitions are rejected.
     */
    fun returnCard(card: PlantCard): Boolean =
        stackFor(card)?.returnCard(card) ?: false

    fun reset() {
        plantStacks.forEach {
            it.reset()
        }
    }

    private fun validate(
        selectedCards: List<PlantCard>
    ) {
        require(selectedCards.size == 9) {
            "Plant Market requires exactly 9 selected cards: ${selectedCards.size}"
        }

        PlantType.entries.forEach { type ->
            val count = selectedCards.count {
                it.type == type
            }

            require(count == 3) {
                "Plant Market requires exactly 3 $type cards: $count"
            }
        }

        val normalizedNames = selectedCards.map {
            it.name.trim().lowercase()
        }

        require(normalizedNames.toSet().size == normalizedNames.size) {
            "Plant Market cannot contain duplicate selected card names"
        }

        // A legal game market is one selected Plant per cost/type slot, not
        // merely three cards of each type. Research may move a Plant between
        // slots; validate its *effective* cost and type rather than its ID.
        val requiredSlots = setOf(
            PlantType.ROOT to 5, PlantType.ROOT to 7, PlantType.ROOT to 9,
            PlantType.VINE to 7, PlantType.VINE to 9, PlantType.VINE to 11,
            PlantType.FLOWER to 11, PlantType.FLOWER to 14, PlantType.FLOWER to 17
        )
        val actualSlots = selectedCards.map { it.type to it.cost }
        require(actualSlots.toSet() == requiredSlots && actualSlots.size == actualSlots.toSet().size) {
            "Plant Market requires exactly one card for each legal type/cost slot: $actualSlots"
        }
    }
}
