package dugsolutions.leaf.simulation.v35.experiment.cardbalance

import dugsolutions.leaf.v35.effect.GameEffect
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForcedRootFourMoreExperimentTest {
    private val rows by lazy { ForcedRootFourMoreExperiment.run() }

    @Test
    fun `forced target is D20 despite size-sorted player Hand`() {
        // PlayerDice sorts the supplied [D20, D6, D4] to [D4, D6, D20].
        // A wrong index-0 choice would affect the D4 in the Low Strike instead.
        assertTrue(rows.all { it.ownStrikeTotal == it.after })
        assertTrue(rows.all { it.before == ForcedRootFourMoreExperiment.scenarios.single { s -> s.id == it.scenario }.ownFace })
    }

    @Test
    fun `matched actual effect resolutions reveal official suppressed and amplified mechanical benefit`() {
        assertEquals(12, rows.size)
        for (scenario in ForcedRootFourMoreExperiment.scenarios) {
            val matched = rows.filter { it.scenario == scenario.id }
            assertEquals(3, matched.size)
            assertEquals(1, matched.map { it.preState }.distinct().size)
            assertTrue(matched.all { it.outputResolutions == 1 })
            assertTrue(matched.all { it.before == scenario.ownFace })
            assertTrue(matched.all { it.opposingStrikeTotal == scenario.opposingFace })
            assertEquals(setOf(
                GameEffect.RAISE_DIE_PLUS_4,
                GameEffect.RESEARCH_NO_EFFECT,
                GameEffect.RESEARCH_RAISE_DIE_PLUS_20
            ), matched.map { it.effectiveEffect }.toSet())
            val suppressed = matched.single { it.condition == ForcedRootFourMoreExperiment.Condition.SUPPRESSED }
            val official = matched.single { it.condition == ForcedRootFourMoreExperiment.Condition.OFFICIAL }
            val amplified = matched.single { it.condition == ForcedRootFourMoreExperiment.Condition.RAISE_20 }
            assertEquals(0, suppressed.actualPips)
            assertEquals(scenario.ownFace, suppressed.after)
            assertEquals(minOf(4, 20 - scenario.ownFace), official.actualPips)
            assertEquals(minOf(20, 20 - scenario.ownFace), amplified.actualPips)
            assertTrue(matched.all { it.immediateVp == 0 })
        }
    }

    @Test
    fun `forced use changes real Strike outcomes without claiming game win lift`() {
        fun case(name: String) = rows.filter { it.scenario == name }
            .associateBy { it.condition }
        val small = case("raise4_turns_loss_to_win")
        assertFalse(small.getValue(ForcedRootFourMoreExperiment.Condition.SUPPRESSED).winner)
        assertTrue(small.getValue(ForcedRootFourMoreExperiment.Condition.OFFICIAL).winner)
        assertTrue(small.getValue(ForcedRootFourMoreExperiment.Condition.RAISE_20).winner)

        val large = case("only_raise20_turns_loss_to_win")
        assertFalse(large.getValue(ForcedRootFourMoreExperiment.Condition.SUPPRESSED).winner)
        assertTrue(large.getValue(ForcedRootFourMoreExperiment.Condition.SUPPRESSED).actorWounded)
        assertFalse(large.getValue(ForcedRootFourMoreExperiment.Condition.OFFICIAL).winner)
        assertFalse(large.getValue(ForcedRootFourMoreExperiment.Condition.OFFICIAL).actorWounded)
        assertTrue(large.getValue(ForcedRootFourMoreExperiment.Condition.RAISE_20).winner)

        val tied = case("raise20_forces_tie")
        assertFalse(tied.getValue(ForcedRootFourMoreExperiment.Condition.RAISE_20).winner)
        assertEquals(0, tied.getValue(ForcedRootFourMoreExperiment.Condition.RAISE_20).strikeVp)

        val capped = case("capped_raises_no_value")
        assertTrue(capped.values.all { it.actualPips == 0 })
        assertEquals(1, capped.values.map { it.strikeVp }.distinct().size)
    }

    @Test
    fun `writes complete tab separated evidence with distinct per condition snapshots`() {
        val path = Files.createTempFile("forced-r4m-", ".tsv")
        try {
            ForcedRootFourMoreExperiment.writeTsv(path, rows)
            val content = Files.readAllLines(path)
            assertEquals(13, content.size)
            assertEquals(ForcedRootFourMoreExperiment.TSV_HEADER, content.first())
            assertTrue(content.drop(1).all { it.split('\t').size == 15 })
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
