package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class DecisionScoreTest {
    @Test fun `fixed and sampled ranges retain an exact audit trail`() {
        var draws = 0
        val rng = StrategyRandomizer { bound -> draws++; assertEquals(16, bound); 11 }
        val score = DecisionScoreBuilder("compost", rng)
            .add(ScoreInfluence("compost.base", "Original base", ScoreValue.Fixed(75)))
            .add(ScoreInfluence("compost.low_roll", "Low target", ScoreValue.Range(0, 15)))
            .build()
        assertEquals(86, score.total)
        assertEquals(1, draws)
        assertEquals(86, score.toPriorityScore().total)
        assertTrue(score.explanation().contains("range: 0..15"))
        score.explanation()
        assertEquals(1, draws)
    }

    @Test fun `range boundaries and parsing`() {
        assertEquals(ScoreValue.Range(-10, 15), ScoreValue.parse("-10..15"))
        assertEquals(ScoreValue.Fixed(-40), ScoreValue.parse("-40"))
        assertThrows(IllegalArgumentException::class.java) { ScoreValue.Range(3, 2) }
    }

    @Test fun `experiment configuration selects only the policy mode`() {
        val props = Properties().apply {
            setProperty("cultivation.main.policy", "HUMAN_WHEN_COMPOST_AVAILABLE")
        }
        val config = DecisionExperimentConfiguration.fromProperties(props)
        assertEquals(CultivationMainPolicyMode.HUMAN_WHEN_COMPOST_AVAILABLE, config.cultivationMainPolicy)
        props.setProperty("external.compost", "20")
        assertThrows(IllegalArgumentException::class.java) {
            DecisionExperimentConfiguration.fromProperties(props)
        }
    }

    @Test fun `baseline score comes from config and is overridable`() {
        val config = HumanScoringConfiguration.fromProperties(Properties().apply {
            setProperty("compost.base", "85")
        })
        assertEquals(ScoreValue.Fixed(85), config.value("compost.base", ScoreValue.Fixed(75)))
        assertEquals(ScoreValue.Fixed(95), config.withValue("compost.base", ScoreValue.Fixed(95))
            .value("compost.base", ScoreValue.Fixed(75)))
    }
}
