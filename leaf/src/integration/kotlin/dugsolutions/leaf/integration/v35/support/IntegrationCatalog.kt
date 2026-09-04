package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.wisp.domain.WispCard
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Loads the real v35 CSV definitions into the production registries/managers
 * used by an [IntegrationGameHarness].
 *
 * Nothing in this class invents card definitions for tests. Integration tests
 * therefore fail when the production CSV/catalog contract is broken.
 */
class IntegrationCatalog(
    private val plantRegistry: PlantCardRegistry,
    private val plantManager: PlantCardManager,
    private val wispRegistry: WispCardRegistry,
    private val wispManager: WispCardManager,
    private val roundRegistry: RoundCardRegistry,
    private val roundManager: RoundCardManager,
    val dataRoot: Path = defaultDataRoot()
) {
    companion object {
        /** Stable CSV keys for the recommended first-game Grove. */
        val FIRST_GAME_PLANT_NAMES: List<String> = listOf(
            "Root_05_02", // Root Four More
            "Root_07_04", // Root Recall
            "Root_09_03", // Root Kindred
            "Vine_07_01", // Berry Important
            "Vine_09_01", // Low & Behold
            "Vine_11_04", // Vine's the Limit
            "Flower_11_01", // Sapping Snapdragon
            "Flower_14_02", // Bloom Backbone
            "Flower_17_04" // Queen's Blossom
        )

        fun defaultDataRoot(): Path =
            Paths.get("data", "v35")
    }

    fun load(): IntegrationCatalog {
        plantRegistry.clear()
        plantRegistry.loadFromCsv(
            path(CardDataFiles.ROOT_CARD_LIST),
            path(CardDataFiles.VF_CARD_LIST)
        )
        plantManager.loadCards(plantRegistry)

        wispRegistry.clear()
        wispRegistry.loadFromCsv(path(CardDataFiles.WISP_LIST))
        wispManager.loadCards(wispRegistry)

        roundRegistry.clear()
        roundRegistry.loadFromCsv(path(CardDataFiles.ROUND_CARD_LIST))
        roundManager.loadCards(roundRegistry)

        return this
    }

    val allPlants: List<PlantCard>
        get() = plantManager.getAllCards().cards

    val allWisps: List<WispCard>
        get() = wispManager.getAllCards().cards

    val allRounds: List<RoundCard>
        get() = roundManager.getAllCards().cards

    val firstGamePlants: List<PlantCard>
        get() = plants(FIRST_GAME_PLANT_NAMES)

    fun plant(nameOrTitle: String): PlantCard? =
        plantManager.getCard(nameOrTitle)
            ?: allPlants.firstOrNull {
                it.title.equals(nameOrTitle, ignoreCase = true)
            }

    fun requirePlant(nameOrTitle: String): PlantCard =
        requireNotNull(plant(nameOrTitle)) {
            "Plant card not found in v35 integration catalog: $nameOrTitle"
        }

    fun plants(namesOrTitles: List<String>): List<PlantCard> =
        namesOrTitles.map(::requirePlant)

    fun wisp(nameOrTitle: String): WispCard? =
        wispManager.getCard(nameOrTitle)
            ?: allWisps.firstOrNull {
                it.title.equals(nameOrTitle, ignoreCase = true)
            }

    fun requireWisp(nameOrTitle: String): WispCard =
        requireNotNull(wisp(nameOrTitle)) {
            "Wisp card not found in v35 integration catalog: $nameOrTitle"
        }

    fun round(name: String): RoundCard? =
        roundManager.getCard(name)

    fun requireRound(name: String): RoundCard =
        requireNotNull(round(name)) {
            "Round card not found in v35 integration catalog: $name"
        }

    private fun path(fileName: String): String =
        dataRoot.resolve(fileName).toString()
}
