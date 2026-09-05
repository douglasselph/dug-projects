package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaselineScoreEngineTest {

    @Test
    fun priorityScore_totalIncludesBaseAndAllAdjustments() {
        val score = PriorityScore(
            base = 55,
            adjustments = listOf(
                ScoreAdjustment(+57, "Flip D20 1 to 20"),
                ScoreAdjustment(+20, "Reaches Flower-17 purchase tier"),
                ScoreAdjustment(-10, "Consumes a reserved resource")
            )
        )

        assertEquals(122, score.total)
    }

    @Test
    fun adjusted_returnsNewScoreWithoutChangingOriginal() {
        val original = PriorityScore(base = 45)
        val changed = original.adjusted(+20, "Low die is attractive to Mulch")

        assertEquals(45, original.total)
        assertEquals(65, changed.total)
        assertEquals(1, changed.adjustments.size)
    }

    @Test
    fun explanation_preservesReasonsAndComputedTotal() {
        val choice = ScoredChoice(
            choice = "Root Cause -> D20 showing 1",
            score = PriorityScore(55)
                .adjusted(+57, "Flip D20 1 to 20")
                .adjusted(+20, "Reaches Flower-17 purchase tier")
        )

        val explanation = choice.explanation(baseReason = "Base effect")

        assertEquals("Root Cause -> D20 showing 1", explanation.label)
        assertEquals(
            listOf("Base effect", "Flip D20 1 to 20", "Reaches Flower-17 purchase tier"),
            explanation.lines.map { it.reason }
        )
        assertEquals(listOf(55, 57, 20), explanation.lines.map { it.amount })
        assertEquals(132, explanation.total)
        assertEquals(
            "Root Cause -> D20 showing 1\n" +
                "55  Base effect\n" +
                "+57  Flip D20 1 to 20\n" +
                "+20  Reaches Flower-17 purchase tier\n" +
                "Total  132",
            explanation.render()
        )
    }

    @Test
    fun choose_uniqueHighestDoesNotConsumeStrategyRandomness() {
        val randomizer = RecordingStrategyRandomizer(next = 1)
        val engine = BaselineScoreEngine(randomizer)

        val chosen = engine.choose(
            listOf(
                scored("low", 10),
                scored("high", 20),
                scored("middle", 15)
            )
        )

        assertEquals("high", chosen.choice)
        assertEquals(0, randomizer.calls)
    }

    @Test
    fun choose_equalHighestUsesStrategyRandomnessOnlyForTiedLeaders() {
        val randomizer = RecordingStrategyRandomizer(next = 1)
        val engine = BaselineScoreEngine(randomizer)

        val chosen = engine.choose(
            listOf(
                scored("lower", 90),
                scored("first tied leader", 100),
                scored("second tied leader", 100)
            )
        )

        assertEquals("second tied leader", chosen.choice)
        assertEquals(1, randomizer.calls)
        assertEquals(2, randomizer.lastUntil)
    }

    @Test
    fun choose_rejectsEmptyCandidateSet() {
        assertFailsWith<IllegalArgumentException> {
            BaselineScoreEngine().choose<String>(emptyList())
        }
    }

    @Test
    fun scoreAdjustment_requiresExplanationReason() {
        assertFailsWith<IllegalArgumentException> {
            ScoreAdjustment(+10, "   ")
        }
    }

    private fun scored(
        value: String,
        total: Int
    ): ScoredChoice<String> =
        ScoredChoice(
            choice = value,
            score = PriorityScore(total)
        )

    private class RecordingStrategyRandomizer(
        private val next: Int
    ) : StrategyRandomizer {
        var calls: Int = 0
            private set
        var lastUntil: Int? = null
            private set

        override fun nextInt(until: Int): Int {
            calls++
            lastUntil = until
            require(next in 0 until until)
            return next
        }
    }
}
