package dugsolutions.leaf.integration.v35.sanity.scoring

import dugsolutions.leaf.integration.v35.support.FinalScoringAssertions
import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.giveButterfly
import dugsolutions.leaf.integration.v35.support.giveNextWisp
import dugsolutions.leaf.integration.v35.support.graftPlant
import dugsolutions.leaf.integration.v35.support.player
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.tokens.Butterfly
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Exact-state integration coverage for the complete v35 final-scoring rule.
 *
 * No rounds are executed here. Each scenario constructs a known Player/Grove
 * state from the real CSV-backed production catalog and invokes the production
 * FinalScorer through IntegrationGameHarness.
 */
class FinalScoringSanityTest {

    @Test
    fun `exact final state scores existing VP fixed and variable Plants and unplayed Wisps independently`() {
        val randomizer = ScriptedRandomizer()
        scoringHarness(
            exactWisps = listOf(
                "Wisp_Award_VP",  // Wisp of Honor = 2 end-game VP
                "Wisp_Award_VP2", // Berry Patient = 3 end-game VP
                "Wisp_Award_VP"   // second physical Wisp of Honor
            ),
            randomizer = randomizer
        ).use { harness ->
            // P1 exact state.
            harness.player(1).addVp(4)
            harness.graftPlant(1, "Root Four More") // legal Root anchor; fixed 1 VP
            harness.graftPlant(1, "Berry Tasty")    // fixed 3 VP Vine
            harness.graftPlant(1, "Vine Yield")     // 1 VP per grafted Flower
            harness.graftPlant(1, "Low & Behold")   // fixed 1 VP Vine
            harness.graftPlant(1, "Alluring Nectar") // 1 VP per Butterfly; also a Flower
            harness.giveButterfly(1, Butterfly.GREEN)
            harness.giveButterfly(1, Butterfly.PURPLE)
            assertEquals("Wisp of Honor", harness.giveNextWisp(1).title)
            assertEquals("Berry Patient", harness.giveNextWisp(1).title)

            // P2 exact state provides an independently checkable lower score.
            harness.player(2).addVp(6)
            harness.graftPlant(2, "Root Four More") // fixed 1 VP
            assertEquals("Wisp of Honor", harness.giveNextWisp(2).title)

            // Prove the integration state is using the structured rules parsed
            // from the real CSV definitions rather than synthetic test cards.
            assertEquals(
                PlantScoringRule.Fixed(3),
                harness.catalog.requirePlant("Berry Tasty").scoringRule
            )
            assertEquals(
                PlantScoringRule.PerGraftedFlower,
                harness.catalog.requirePlant("Vine Yield").scoringRule
            )
            assertEquals(
                PlantScoringRule.PerButterfly,
                harness.catalog.requirePlant("Alluring Nectar").scoringRule
            )
            assertEquals(2, harness.catalog.requireWisp("Wisp of Honor").endGameVp)
            assertEquals(3, harness.catalog.requireWisp("Berry Patient").endGameVp)

            val result = harness.scoreFinalState()

            // P1 Plant VP = Root Four More 1 + Berry Tasty 3
            //             + Vine Yield 1 (one grafted Flower)
            //             + Low & Behold 1 + Alluring Nectar 2 = 8.
            // Wisp VP = 2 + 3 = 5. Total = 4 + 8 + 5 = 17.
            FinalScoringAssertions.assertScore(
                result = result,
                playerId = 1,
                existingVp = 4,
                plantVp = 8,
                unplayedWispVp = 5,
                totalVp = 17,
                graftedPlantCount = 5
            )

            // P2 = 6 existing + 1 Root + 2 Wisp = 9.
            FinalScoringAssertions.assertScore(
                result = result,
                playerId = 2,
                existingVp = 6,
                plantVp = 1,
                unplayedWispVp = 2,
                totalVp = 9,
                graftedPlantCount = 1
            )
            FinalScoringAssertions.assertWinners(result, 1)

            // Scoring itself must be pure/deterministic: it consumes neither
            // random values nor any player/Grove components.
            val before = harness.snapshot()
            val secondResult = harness.scoreFinalState()
            assertEquals(result, secondResult)
            assertEquals(before, harness.snapshot())
            randomizer.assertExhausted()
        }
    }

    @Test
    fun `equal total VP is broken by most grafted Plant cards`() {
        val randomizer = ScriptedRandomizer()
        scoringHarness(randomizer = randomizer).use { harness ->
            // P1: 10 total, zero Plants.
            harness.player(1).addVp(10)

            // P2: 6 existing + Berry Tasty 3 + Root Four More 1 = 10,
            // but with two grafted Plants, so P2 wins the tiebreaker.
            harness.player(2).addVp(6)
            harness.graftPlant(2, "Root Four More")
            harness.graftPlant(2, "Berry Tasty")

            val result = harness.scoreFinalState()

            FinalScoringAssertions.assertScore(result, 1, 10, 0, 0, 10, 0)
            FinalScoringAssertions.assertScore(result, 2, 6, 4, 0, 10, 2)
            FinalScoringAssertions.assertWinners(result, 2)
            randomizer.assertExhausted()
        }
    }

    @Test
    fun `equal total VP and equal Plant count produces shared victory`() {
        val randomizer = ScriptedRandomizer()
        scoringHarness(randomizer = randomizer).use { harness ->
            // Both finish on 10 VP with two grafted Plants.
            // Each starts with a Root, then legally attaches a Vine.
            harness.player(1).addVp(6)
            harness.graftPlant(1, "Root Four More") // +1
            harness.graftPlant(1, "Berry Tasty")     // +3

            harness.player(2).addVp(8)
            harness.graftPlant(2, "Root Four More") // +1
            harness.graftPlant(2, "Low & Behold")   // +1

            val result = harness.scoreFinalState()

            FinalScoringAssertions.assertScore(result, 1, 6, 4, 0, 10, 2)
            FinalScoringAssertions.assertScore(result, 2, 8, 2, 0, 10, 2)
            FinalScoringAssertions.assertWinners(result, 1, 2)
            randomizer.assertExhausted()
        }
    }

    @Test
    fun `variable Vine Yield counts grafted Flowers rather than Vines`() {
        scoringHarness().use { harness ->
            harness.graftPlant(1, "Root Four More")
            harness.graftPlant(1, "Berry Tasty")
            harness.graftPlant(1, "Vine Yield")
            harness.graftPlant(1, "Low & Behold")
            harness.graftPlant(1, "Bloom Backbone") // one Flower

            val result = harness.scoreFinalState()
            val score = FinalScoringAssertions.scoreFor(result, 1)

            // Root Four More 1 + Berry Tasty 3 + Vine Yield 1
            // (one Flower) + Low & Behold 1 + Bloom Backbone 1.
            assertEquals(7, score.plantVp)
            assertEquals(5, score.graftedPlantCount)
        }
    }

    private fun scoringHarness(
        exactWisps: List<String> = emptyList(),
        randomizer: ScriptedRandomizer = ScriptedRandomizer()
    ): IntegrationGameHarness =
        IntegrationGameHarness(
            GameScenario(
                numPlayers = 2,
                roundSetup = GameRoundSetup.Ordered(
                    cultivationRounds = 1,
                    battleRounds = 0
                ),
                exactRoundNames = listOf("Resource_Compost_Mulch"),
                exactWispNames = exactWisps,
                randomizerFactory = { randomizer }
            )
        )

}
