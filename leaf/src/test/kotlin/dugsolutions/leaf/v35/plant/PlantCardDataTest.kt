package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract tests for the authored v35 Plant card data.
 *
 * These are intentionally tied to the CSV source of truth. They verify design
 * and naming invariants rather than the implementation of PlantCardRegistry.
 */
class PlantCardDataTest {

    private lateinit var cards: List<PlantCard>

    @BeforeEach
    fun setup() {
        val registry = PlantCardRegistry()
        registry.loadFromCsv(
            dataPath(CardDataFiles.ROOT_CARD_LIST),
            dataPath(CardDataFiles.VF_CARD_LIST)
        )
        cards = registry.getAllCards()
    }

    @Test
    fun cardNames_matchTypeCostIndexConvention() {
        // Arrange
        val pattern = Regex("""^(Root|Vine|Flower)_(\d{2})_(\d{2})$""")

        // Act / Assert
        cards.forEach { card ->
            val match = assertNotNull(
                pattern.matchEntire(card.name),
                "Plant card name does not match TYPE_COST_INDEX: ${card.name}"
            )

            val typeInName = match.groupValues[1]
            val costInName = match.groupValues[2].toInt()
            val indexInName = match.groupValues[3].toInt()

            assertEquals(
                card.type.name,
                typeInName.uppercase(),
                "Type encoded in '${card.name}' does not match ${card.type}"
            )
            assertEquals(
                card.cost,
                costInName,
                "Cost encoded in '${card.name}' does not match ${card.cost}"
            )
            assertTrue(
                indexInName in 1..4,
                "Index encoded in '${card.name}' must be 01 through 04"
            )
        }
    }

    @Test
    fun plantDeck_hasExactlyExpectedTypeAndCostTiers() {
        // Arrange
        val expected = mapOf(
            PlantType.ROOT to setOf(5, 7, 9),
            PlantType.VINE to setOf(7, 9, 11),
            PlantType.FLOWER to setOf(11, 14, 17)
        )

        // Act
        val actual = cards
            .groupBy { it.type }
            .mapValues { (_, cardsOfType) ->
                cardsOfType.map { it.cost }.toSet()
            }

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun eachTypeAndCostTier_hasExactlyFourCards() {
        // Act
        val groups = cards.groupBy { it.type to it.cost }

        // Assert
        groups.forEach { (typeAndCost, cardsInTier) ->
            assertEquals(
                4,
                cardsInTier.size,
                "Expected exactly 4 cards for ${typeAndCost.first} cost ${typeAndCost.second}"
            )
        }

        assertEquals(9, groups.size)
        assertEquals(36, cards.size)
    }

    @Test
    fun eachTypeAndCostTier_hasIndexes01Through04ExactlyOnce() {
        // Arrange
        val pattern = Regex("""^(Root|Vine|Flower)_(\d{2})_(\d{2})$""")

        // Act
        val groups = cards.groupBy { it.type to it.cost }

        // Assert
        groups.forEach { (typeAndCost, cardsInTier) ->
            val indexes = cardsInTier.map { card ->
                val match = assertNotNull(
                    pattern.matchEntire(card.name),
                    "Invalid Plant card name: ${card.name}"
                )
                match.groupValues[3].toInt()
            }.sorted()

            assertEquals(
                listOf(1, 2, 3, 4),
                indexes,
                "Expected indexes 01-04 for ${typeAndCost.first} cost ${typeAndCost.second}"
            )
        }
    }

    @Test
    fun cardNames_areUniqueIgnoringCase() {
        // Act
        val normalizedNames = cards.map { it.name.lowercase() }

        // Assert
        assertEquals(
            normalizedNames.size,
            normalizedNames.toSet().size,
            "Plant card names must be unique ignoring case"
        )
    }

    private fun dataPath(fileName: String): String =
        Path.of("data", "v35", fileName).toString()
}
