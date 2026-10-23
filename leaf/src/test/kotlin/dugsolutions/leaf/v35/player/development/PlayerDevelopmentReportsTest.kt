package dugsolutions.leaf.v35.player.development

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class PlayerDevelopmentReportsTest {
    private val catalog = listOf(
        DevelopmentPlantIdentity("Queen's Blossom", "Flower_17_01", "FLOWER", 17),
        DevelopmentPlantIdentity("Root Well", "Root_05_01", "ROOT", 5),
        DevelopmentPlantIdentity("Sapping Snapdragon", "Flower_11_03", "FLOWER", 11)
    )
    private val expert = PlayerDevelopmentResult("g1", "p0", 2, "LEARNED", "learner-a", true, 42, 5,
        PlayerDevelopmentSnapshot(
            listOf(
                PlantDevelopment(10, "Root Well", "Root_05_01", "ROOT", 5, "LEFT", 0, 0, "GRAFTED"),
                PlantDevelopment(11, "Root Well", "Root_05_01", "ROOT", 5, "RIGHT", 1, 0, "SNIPPED"),
                PlantDevelopment(12, "Queen's Blossom", "Flower_17_01", "FLOWER", 17, "TOP", 1, 2, "GRAFTED")
            ), listOf(
                DieDevelopment(100, 4, 8, "OWNED", 1),
                DieDevelopment(101, 12, 12, "DOOM", 0)
            )
        ))
    private val baseline = PlayerDevelopmentResult("g1", "p1", 2, "CONTROL", "baseline", false, 32, 3,
        PlayerDevelopmentSnapshot(emptyList(), listOf(DieDevelopment(200, 4, 4, "OWNED", 0))))

    @Test fun preservesPhysicalCopiesAndNamedCardFates() {
        val reports = PlayerDevelopmentReports.generate(listOf(expert, baseline), catalog).associate { it.fileName to it.tsv }
        val individual = reports.getValue("development-plants.tsv")
        assertTrue(individual.contains("10\tRoot Well\tRoot_05_01"))
        assertTrue(individual.contains("11\tRoot Well\tRoot_05_01"))
        assertTrue(individual.contains("SNIPPED"))
        val summary = reports.getValue("development-card-summary.tsv")
        val row = summary.lines().first { it.startsWith("2\tLEARNED\tlearner-a\tALL\t1\tRoot Well\t") }
        assertTrue(row.contains("\t2\t1\t2.0000\t100.0000\t1\t1\t0"), row)
        // Zero-acquisition cards must not disappear from the report.
        assertTrue(summary.lines().any { it.startsWith("2\tLEARNED\tlearner-a\tALL\t1\tSapping Snapdragon\t") && it.contains("\t0\t0\t0.0000") })
    }

    @Test fun separatesPoliciesAndWinLossCohorts() {
        val reports = PlayerDevelopmentReports.generate(listOf(expert, baseline)).associate { it.fileName to it.tsv }
        val shape = reports.getValue("development-shape-summary.tsv")
        assertTrue(shape.lines().any { it.startsWith("2\tLEARNED\tlearner-a\tWIN\t1\t") })
        assertTrue(shape.lines().any { it.startsWith("2\tCONTROL\tbaseline\tLOSS\t1\t") })
        assertTrue(!shape.lines().any { it.startsWith("2\tLEARNED\tlearner-a\tLOSS\t") })
        val dice = reports.getValue("development-dice-summary.tsv")
        assertTrue(dice.contains("\tDOOM" ).not()) // Summary is grouped by fate counts, not arbitrary labels.
        assertTrue(reports.getValue("development-dice.tsv").contains("101\t12\t12\tDOOM\t0"))
        assertTrue(reports.getValue("development-players.tsv").contains("42\t5\t3\t2\t1"))
    }

    @Test fun writesStableReportsAndRejectsDuplicatePlayers() {
        val dir = Files.createTempDirectory("development-d7-")
        try {
            val paths = PlayerDevelopmentReports.write(listOf(expert, baseline), dir, catalog)
            assertEquals(6, paths.size)
            assertTrue(paths.all { Files.size(it) > 0 })
            assertEquals(PlayerDevelopmentReports.generate(listOf(expert, baseline), catalog).map { it.tsv },
                paths.map { Files.readString(it) })
            assertFailsWith<IllegalArgumentException> { PlayerDevelopmentReports.generate(listOf(expert, expert)) }
        } finally {
            Files.list(dir).use { stream -> stream.forEach { Files.deleteIfExists(it) } }
            Files.deleteIfExists(dir)
        }
    }
}
