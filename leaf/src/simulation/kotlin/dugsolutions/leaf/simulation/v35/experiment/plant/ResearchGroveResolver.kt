package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantValueResolver
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.random.Randomizer

/**
 * Resolves a research Grove using a dedicated Grove RNG stream.
 *
 * A null [grovePattern] preserves the fixed default Grove. Otherwise every zero
 * slot in the pattern is resolved from the legal numbered alternatives while
 * respecting explicit exclusions and Plant experiment availability.
 *
 * The resolver is deterministic for a given [groveSeed] + [sample] and never
 * consumes the game's mechanical or strategy RNG streams.
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
    val unavailable = allPlants.filterNot(plantValues::isAvailable).map { it.name }.toSet()
    val excluded = excludedCards + unavailable

    if (grovePattern == null) {
        require(excludedCards.isEmpty()) { "Explicit Plant exclusions require a Grove pattern; the default Grove is fixed." }
        val blocked = defaultGrove.filterNot(plantValues::isAvailable)
        require(blocked.isEmpty()) {
            "Default Grove fixes Plant cards that are unavailable in the active Plant experiment: ${blocked.map { it.name }.sorted()}"
        }
        return defaultGrove
    }

    val pattern = GrovePlantCode.validate(grovePattern)
    val fixedNames = GrovePlantCode.overrideNames(pattern)
    val fixedNameSet = fixedNames.toSet()
    val blockedFixed = fixedNames.filter { it in excluded }
    require(blockedFixed.isEmpty()) {
        "Grove pattern $pattern explicitly fixes unavailable/excluded Plant cards: ${blockedFixed.sorted()}"
    }

    pattern.forEachIndexed { index, digit ->
        if (digit == '0') {
            val candidates = ('1'..'4').map { candidateDigit ->
                val chars = pattern.toCharArray()
                chars[index] = candidateDigit
                val expandedNames = GrovePlantCode.overrideNames(chars.concatToString())
                (expandedNames.toSet() - fixedNameSet).single()
            }
            require(candidates.any { it !in excluded }) {
                "No Plant cards remain available for Grove pattern slot ${index + 1}; excluded/unavailable candidates=${candidates.sorted()}"
            }
        }
    }

    val randomizer = Randomizer.create(groveSeed + sample)
    repeat(10_000) {
        val concreteCode = GrovePlantCode.generate(pattern, randomizer)
        val names = GrovePlantCode.overrideNames(concreteCode)
        if (names.none { it in excluded }) {
            return names.map { name -> requireNotNull(plantManager.getCard(name)) }
        }
    }
    error("Could not resolve Grove pattern $pattern without unavailable/excluded cards ${excluded.sorted()}; constraints may eliminate every choice in a slot.")
}
