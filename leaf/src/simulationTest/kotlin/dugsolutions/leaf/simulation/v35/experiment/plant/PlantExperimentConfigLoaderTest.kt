package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlantExperimentConfigLoaderTest {

    private val cards = listOf(
        plantCard("Vine_07_01", 7, PlantScoringRule.Fixed(3)),
        plantCard("Vine_07_04", 7, PlantScoringRule.PerGraftedVine),
        plantCard("Vine_09_03", 9, PlantScoringRule.Fixed(2))
    )
    private val loader = PlantExperimentConfigLoader(cards)

    @Test
    fun emptyFile_isNoOp() {
        val config = loadCsv("")

        assertTrue(config.isEmpty)
    }

    @Test
    fun headerOnlyFile_isNoOp() {
        val config = loadCsv("card_id,cost,available,scoring\n")

        assertTrue(config.isEmpty)
    }

    @Test
    fun oneCostOverride_loadsByStablePlantId() {
        val config = loadCsv(
            """
            card_id,cost,available,scoring
            Vine_07_01,11,,
            """.trimIndent()
        )

        val override = requireNotNull(config.overrideFor("Vine_07_01"))
        assertEquals(11, override.cost)
        assertNull(override.available)
        assertNull(override.scoringRule)
    }

    @Test
    fun multipleCards_loadIndependentOverrides() {
        val config = loadCsv(
            """
            card_id,cost,available,scoring
            Vine_07_01,11,true,
            Vine_07_04,,false,
            Vine_09_03,7,,
            """.trimIndent()
        )

        assertEquals(11, config.overrideFor("Vine_07_01")?.cost)
        assertEquals(true, config.overrideFor("Vine_07_01")?.available)
        assertEquals(false, config.overrideFor("Vine_07_04")?.available)
        assertEquals(7, config.overrideFor("Vine_09_03")?.cost)
    }

    @Test
    fun availabilityFalse_isExplicitExclusionDimension() {
        val config = loadCsv(
            """
            card_id,cost,available,scoring
            Vine_07_04,,false,
            """.trimIndent()
        )

        val override = requireNotNull(config.overrideFor("Vine_07_04"))
        assertFalse(requireNotNull(override.available))
        assertNull(override.cost)
    }

    @Test
    fun zeroCost_remainsCostAndDoesNotImplyExclusion() {
        val config = loadCsv(
            """
            card_id,cost,available,scoring
            Vine_07_01,0,,
            """.trimIndent()
        )

        val override = requireNotNull(config.overrideFor("Vine_07_01"))
        assertEquals(0, override.cost)
        assertNull(override.available)
    }

    @Test
    fun blankFields_createNoOverrideForThatCard() {
        val config = loadCsv(
            """
            card_id,cost,available,scoring
            Vine_07_01,,,
            """.trimIndent()
        )

        assertTrue(config.isEmpty)
        assertNull(config.overrideFor("Vine_07_01"))
    }

    @Test
    fun unknownPlantId_failsClearly() {
        val error = assertFailsWith<IllegalArgumentException> {
            loadCsv(
                """
                card_id,cost,available,scoring
                Vine_99_99,7,,
                """.trimIndent()
            )
        }

        assertTrue(error.message.orEmpty().contains("Unknown Plant ID 'Vine_99_99'"))
    }

    @Test
    fun duplicatePlantId_failsRatherThanLastWriteWins() {
        val error = assertFailsWith<IllegalArgumentException> {
            loadCsv(
                """
                card_id,cost,available,scoring
                Vine_07_01,8,,
                  VINE_07_01  ,9,,
                """.trimIndent()
            )
        }

        assertTrue(error.message.orEmpty().contains("Duplicate Plant ID"))
    }

    @Test
    fun malformedCost_failsClearly() {
        val error = assertFailsWith<IllegalArgumentException> {
            loadCsv(
                """
                card_id,cost,available,scoring
                Vine_07_01,seven,,
                """.trimIndent()
            )
        }

        assertTrue(error.message.orEmpty().contains("Invalid cost 'seven'"))
    }

    @Test
    fun negativeCost_failsClearly() {
        val error = assertFailsWith<IllegalArgumentException> {
            loadCsv(
                """
                card_id,cost,available,scoring
                Vine_07_01,-1,,
                """.trimIndent()
            )
        }

        assertTrue(error.message.orEmpty().contains("Invalid negative cost '-1'"))
    }

    @Test
    fun malformedBoolean_failsClearly() {
        val error = assertFailsWith<IllegalArgumentException> {
            loadCsv(
                """
                card_id,cost,available,scoring
                Vine_07_01,,yes,
                """.trimIndent()
            )
        }

        assertTrue(error.message.orEmpty().contains("Invalid available Boolean 'yes'"))
    }

    @Test
    fun typedScoringExpressions_mapToExistingPlantScoringRules() {
        val config = loadCsv(
            """
            card_id,cost,available,scoring
            Vine_07_01,,,FIXED:1
            Vine_07_04,,,PER_GRAFTED_FLOWER
            Vine_09_03,,,PER_BUTTERFLY
            """.trimIndent()
        )

        assertEquals(PlantScoringRule.Fixed(1), config.overrideFor("Vine_07_01")?.scoringRule)
        assertEquals(PlantScoringRule.PerGraftedFlower, config.overrideFor("Vine_07_04")?.scoringRule)
        assertEquals(PlantScoringRule.PerButterfly, config.overrideFor("Vine_09_03")?.scoringRule)
    }

    @Test
    fun perOwnedD4ScoringExpression_isSupported() {
        val config = loadCsv(
            """
            card_id,cost,available,scoring
            Vine_09_03,,,PER_OWNED_D4
            """.trimIndent()
        )

        assertEquals(PlantScoringRule.PerOwnedD4, config.overrideFor("Vine_09_03")?.scoringRule)
    }

    @Test
    fun bareIntegerScoring_failsInsteadOfFlatteningTypedScoringToInteger() {
        val error = assertFailsWith<IllegalArgumentException> {
            loadCsv(
                """
                card_id,cost,available,scoring
                Vine_07_04,,,3
                """.trimIndent()
            )
        }

        assertTrue(error.message.orEmpty().contains("Unknown Plant scoring expression '3'"))

        val canonical = cards.first { it.name == "Vine_07_04" }
        assertEquals(PlantScoringRule.PerGraftedVine, canonical.scoringRule)
    }

    @Test
    fun malformedFixedScoring_failsClearly() {
        val error = assertFailsWith<IllegalArgumentException> {
            loadCsv(
                """
                card_id,cost,available,scoring
                Vine_07_01,,,FIXED:many
                """.trimIndent()
            )
        }

        assertTrue(error.message.orEmpty().contains("Invalid FIXED Plant scoring expression 'FIXED:many'"))
    }

    @Test
    fun unknownScoringExpression_failsClearly() {
        val error = assertFailsWith<IllegalArgumentException> {
            loadCsv(
                """
                card_id,cost,available,scoring
                Vine_07_04,,,PER_MUSHROOM
                """.trimIndent()
            )
        }

        assertTrue(error.message.orEmpty().contains("Unknown Plant scoring expression 'PER_MUSHROOM'"))
    }

    @Test
    fun arbitraryFilePath_isAccepted() {
        val file = Files.createTempFile("plant-overrides-", ".csv")
        Files.writeString(
            file,
            "card_id,cost,available,scoring\nVine_09_03,10,,\n"
        )

        try {
            val config = loader.load(file.toString())
            assertEquals(10, config.overrideFor("Vine_09_03")?.cost)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun loadCsv(content: String): PlantExperimentConfig {
        val file = Files.createTempFile("plant-overrides-", ".csv")
        Files.writeString(file, content)
        return try {
            loader.load(file.toString())
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun plantCard(
        name: String,
        cost: Int,
        scoringRule: PlantScoringRule
    ): PlantCard =
        PlantCard(
            quantity = 1,
            name = name,
            title = name,
            type = PlantType.VINE,
            cost = cost,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = GameEffect.UNKNOWN,
            scoringRule = scoringRule
        )
}
