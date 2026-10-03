package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantValueResolver
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.random.Randomizer

/**
 * Resolves a research Grove using a dedicated Grove RNG stream.
 *
 * Experimental type/cost overrides participate in slot membership. That means
 * moving a card from VINE 7 to FLOWER 14 removes it from the VINE 7 candidate
 * pool and adds it to the FLOWER 14 candidate pool before random selection.
 *
 * A null [grovePattern] preserves the fixed default Grove. Otherwise every zero
 * slot in the pattern is resolved from the currently legal effective candidates.
 * Non-zero digits still name canonical card IDs and are accepted only when that
 * named card's effective type/cost still matches the requested slot.
 */
internal fun resolveResearchGroveForSample(
    grovePattern: String?,
    groveSeed: Long,
    sample: Int,
    plantManager: PlantCardManager,
    defaultGrove: List<PlantCard>,
    allPlants: List<PlantCard>,
    plantValues: PlantValueResolver = PlantValueResolver.CANONICAL,
    excludedCards: Set<String> = emptySet(),
): List<PlantCard> {
    val knownNames = allPlants.map { it.name }.toSet()
    excludedCards.forEach { require(it in knownNames) { "Unknown excluded Plant: $it" } }

    fun effective(card: PlantCard): PlantCard = plantValues.effectiveCardFor(card)
    fun allowed(card: PlantCard): Boolean =
        card.name !in excludedCards && plantValues.isAvailable(card)

    if (grovePattern == null) {
        require(excludedCards.isEmpty()) {
            "Explicit Plant exclusions require a Grove pattern; the default Grove is fixed."
        }
        val blocked = defaultGrove.filterNot(plantValues::isAvailable)
        require(blocked.isEmpty()) {
            "Default Grove fixes Plant cards that are unavailable in the active Plant experiment: ${blocked.map { it.name }.sorted()}"
        }
        return defaultGrove.map(::effective)
    }

    val pattern = GrovePlantCode.validate(grovePattern)
    val randomizer = Randomizer.create(groveSeed + sample)

    return RESEARCH_GROVE_SLOTS.mapIndexed { index, slot ->
        val digit = pattern[index]
        val canonical = if (digit == '0') {
            val candidates = allPlants
                .filter(::allowed)
                .filter { plantValues.typeFor(it) == slot.type && plantValues.costFor(it) == slot.cost }
            randomizer.randomOrNull(candidates)
                ?: error(
                    "No Plant cards available for Grove slot ${slot.label} after applying Plant experiment " +
                        "type/cost/availability overrides and exclusions"
                )
        } else {
            val namedPattern = CharArray(9) { '0' }.also { it[index] = digit }.concatToString()
            val name = GrovePlantCode.overrideNames(namedPattern).single()
            require(name !in excludedCards) {
                "Grove pattern $pattern explicitly fixes excluded Plant card $name"
            }
            val card = requireNotNull(plantManager.getCard(name)) {
                "Unknown Plant card fixed by Grove pattern: $name"
            }
            require(plantValues.isAvailable(card)) {
                "Grove pattern $pattern explicitly fixes unavailable Plant card $name"
            }
            require(plantValues.typeFor(card) == slot.type && plantValues.costFor(card) == slot.cost) {
                "Grove pattern $pattern fixes $name into ${slot.label}, but its active Plant override moves it to " +
                    "${plantValues.typeFor(card)} ${plantValues.costFor(card)}. Use 0 for that slot when testing type/cost moves."
            }
            card
        }
        effective(canonical)
    }
}

private data class ResearchGroveSlot(val type: PlantType, val cost: Int) {
    val label: String get() = "${type.name} $cost"
}

private val RESEARCH_GROVE_SLOTS = listOf(
    ResearchGroveSlot(PlantType.ROOT, 5),
    ResearchGroveSlot(PlantType.ROOT, 7),
    ResearchGroveSlot(PlantType.ROOT, 9),
    ResearchGroveSlot(PlantType.VINE, 7),
    ResearchGroveSlot(PlantType.VINE, 9),
    ResearchGroveSlot(PlantType.VINE, 11),
    ResearchGroveSlot(PlantType.FLOWER, 11),
    ResearchGroveSlot(PlantType.FLOWER, 14),
    ResearchGroveSlot(PlantType.FLOWER, 17),
)
