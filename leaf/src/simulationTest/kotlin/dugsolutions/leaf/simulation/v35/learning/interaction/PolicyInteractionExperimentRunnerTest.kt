package dugsolutions.leaf.simulation.v35.learning.interaction

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyInteractionExperimentRunnerTest {
    private val root: Path = Path.of(System.getProperty("user.dir"))
    private val script: Path = root.resolve("bin/experiment_policy_interactions")

    @Test
    fun `expert ladder and screen build expected condition sets with matched seeds`() {
        val fixtures = weightFixtures()
        val ladder = createTempDirectory("policy-runner-ladder")
        val ladderResult = runScript(
            "--expert-ladder", "--samples", "2", "--dry-run", "--output-dir", ladder.toString(),
            *weightArgs(fixtures)
        )
        assertEquals(0, ladderResult.exitCode, ladderResult.output)
        val ladderRows = ladder.resolve("condition-table.tsv").readLines()
        assertEquals(9, ladderRows.size) // header + A-H
        assertContains(ladderRows.first(), "battle_main")
        assertTrue(ladderRows.last().startsWith("FULLY_LEARNED\tlearned\tlearned\tlearned\tlearned\tlearned\tlearned\tlearned"))

        val manifest = ladder.resolve("manifest.tsv").readLines()
        assertEquals(9, manifest.size)
        val seedColumns = manifest.first().split('\t').withIndex().associate { it.value to it.index }
        val seeds = manifest.drop(1).map { row ->
            val c = row.split('\t')
            Triple(c[seedColumns.getValue("grove_seed")], c[seedColumns.getValue("mechanical_seed")], c[seedColumns.getValue("strategy_seed")])
        }.distinct()
        assertEquals(1, seeds.size, "all conditions must share the same externally controlled schedules")

        val screen = createTempDirectory("policy-runner-screen")
        val screenResult = runScript(
            "--screen", "--samples", "1", "--dry-run", "--output-dir", screen.toString(),
            *weightArgs(fixtures)
        )
        assertEquals(0, screenResult.exitCode, screenResult.output)
        assertEquals(14, screen.resolve("condition-table.tsv").readLines().size) // header + 13 curated conditions
    }

    @Test
    fun `explicit aliases archive provenance and resume completed conditions`() {
        val fixtures = weightFixtures()
        val out = createTempDirectory("policy-runner-explicit")
        val args = arrayOf(
            "--conditions", "A,H", "--samples", "1", "--dry-run", "--output-dir", out.toString(),
            *weightArgs(fixtures)
        )
        val first = runScript(*args)
        assertEquals(0, first.exitCode, first.output)
        val table = out.resolve("condition-table.tsv").readText()
        assertContains(table, "HUMAN_ALL")
        assertContains(table, "FULLY_LEARNED")
        assertContains(out.resolve("config/experiment.txt").readText(), "rounds=3/2/2")
        assertTrue(Files.isRegularFile(out.resolve("weights/provenance.tsv")))
        assertTrue(Files.isRegularFile(out.resolve("reports/summary.txt")))
        assertTrue(Files.isRegularFile(out.resolve("reports/round-effect-research.tsv")))
        assertTrue(Files.isRegularFile(out.resolve("policy-interactions-full-results.tar.gz")))

        val archiveListing = ProcessBuilder("tar", "-tzf", out.resolve("policy-interactions-full-results.tar.gz").toString())
            .directory(root.toFile()).redirectErrorStream(true).start().let { p ->
                val text = p.inputStream.bufferedReader().readText(); assertEquals(0, p.waitFor(), text); text
            }
        listOf("config/", "weights/", "logs/", "reports/", "manifest.tsv", "condition-table.tsv").forEach {
            assertContains(archiveListing, it)
        }

        val second = runScript(*args)
        assertEquals(0, second.exitCode, second.output)
        assertContains(second.output, "SKIP HUMAN_ALL (complete)")
        assertContains(second.output, "SKIP FULLY_LEARNED (complete)")
    }

    @Test
    fun `unknown condition and missing learned weight fail early while human does not need weights`() {
        val humanOut = createTempDirectory("policy-runner-human")
        val human = runScript("--conditions", "A", "--samples", "1", "--dry-run", "--output-dir", humanOut.toString())
        assertEquals(0, human.exitCode, human.output)

        val unknown = runScript("--conditions", "NOPE", "--samples", "1", "--dry-run", "--output-dir", createTempDirectory("policy-runner-bad").toString())
        assertFalse(unknown.exitCode == 0)
        assertContains(unknown.output, "Unknown condition 'NOPE'")

        val missing = createTempDirectory("policy-runner-missing")
        val learned = runScript(
            "--conditions", "ISOLATED_WISP", "--samples", "1", "--dry-run", "--output-dir", missing.resolve("out").toString(),
            "--wisp-weights", missing.resolve("missing.weights").toString()
        )
        assertFalse(learned.exitCode == 0)
        assertContains(learned.output, "Missing Wisp weights")
    }

    private fun weightFixtures(): Map<String, Path> {
        val dir = createTempDirectory("policy-runner-weights")
        return listOf("buy", "cm", "cs", "wisp", "plant", "bs", "bm").associateWith { name ->
            dir.resolve("$name.weights").also {
                Files.writeString(it, "formatVersion=1\npolicy=$name\ntrainingStatus=test\ntrainedRoundPattern=3/2/2\n")
            }
        }
    }

    private fun weightArgs(f: Map<String, Path>) = arrayOf(
        "--buy-weights", f.getValue("buy").toString(),
        "--cultivation-main-weights", f.getValue("cm").toString(),
        "--cultivation-support-weights", f.getValue("cs").toString(),
        "--wisp-weights", f.getValue("wisp").toString(),
        "--plant-effect-weights", f.getValue("plant").toString(),
        "--battle-support-weights", f.getValue("bs").toString(),
        "--battle-main-weights", f.getValue("bm").toString()
    )

    private fun runScript(vararg args: String): Result {
        val command = mutableListOf("bash", script.toString()).apply { addAll(args) }
        val process = ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        return Result(process.waitFor(), output)
    }

    private data class Result(val exitCode: Int, val output: String)
}
