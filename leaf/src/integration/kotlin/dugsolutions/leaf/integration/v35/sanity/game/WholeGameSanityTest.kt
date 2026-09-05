package dugsolutions.leaf.integration.v35.sanity.game

import dugsolutions.leaf.integration.v35.support.FinalScoringAssertions
import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationCatalog
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.WholeGameAssertions
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WholeGameSanityTest {

    @Test
    fun `one Cultivation plus one Battle fully scripted produces exact final score`() {
        val first = scriptedMiniGamePlayer()
        val second = scriptedMiniGamePlayer()
        val randomizer = ScriptedRandomizer().rolls(
            // Cultivation opening Draws.
            4, 4, 4,
            3, 3, 3,
            // P1 then P2 each take two Draw Main Actions.
            6, 6,
            5, 5,
            // Battle opening Draws after Supply refill behavior.
            6, 4, 4,
            5, 3, 3,
            // Battle First Main Draws, then Final Main Draws.
            4, 3,
            6, 5
        )

        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 1,
                battleRounds = 1
            ),
            exactRoundNames = listOf(
                "Resource_Sunlight_Water",
                "Battle_Bloom_Burrow"
            ),
            exactWispNames = emptyList(),
            randomizerFactory = { randomizer },
            decisionFactories = listOf(
                first.singleGameFactory(),
                second.singleGameFactory()
            )
        )

        IntegrationGameHarness(scenario).use { harness ->
            val result = harness.runGame()

            WholeGameAssertions.assertCompletedGame(
                harness = harness,
                result = result,
                expectedCultivationRounds = 1,
                expectedBattleRounds = 1
            )

            FinalScoringAssertions.assertScore(
                result = result.finalScoring,
                playerId = 1,
                existingVp = 6,
                plantVp = 0,
                unplayedWispVp = 0,
                totalVp = 6,
                graftedPlantCount = 0
            )
            FinalScoringAssertions.assertScore(
                result = result.finalScoring,
                playerId = 2,
                existingVp = 0,
                plantVp = 0,
                unplayedWispVp = 0,
                totalVp = 0,
                graftedPlantCount = 0
            )
            FinalScoringAssertions.assertWinners(result.finalScoring, 1)

            val entries = harness.chronicleEntries()
            assertEquals(
                listOf(RoundCardType.CULTIVATION, RoundCardType.BATTLE),
                entries.filterIsInstance<GameEntry.RoundRevealed>().map { it.cardType }
            )
            assertEquals(
                listOf(PlayerId(1), PlayerId(1), PlayerId(1)),
                entries.filterIsInstance<GameEntry.StrikeResolved>().map { it.winnerIds.single() }
            )
            assertEquals(6, entries.filterIsInstance<GameEntry.StrikeResolved>().sumOf { it.vpPerWinner })

            randomizer.assertExhausted()
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `first-game 6 Cultivation plus 3 Battle baseline game completes with coherent lifecycle`() {
        val scenario = GameScenario(
            numPlayers = 4,
            selectedPlantNames = IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
            roundSetup = GameRoundSetup.firstGame(),
            seed = 13_579L
        )

        IntegrationGameHarness(scenario).use { harness ->
            val initial = harness.snapshot()
            assertEquals(
                IntegrationCatalog.FIRST_GAME_PLANT_NAMES.toSet(),
                initial.grove.plantStacks.map { it.name }.toSet()
            )

            val result = harness.runGame()

            WholeGameAssertions.assertCompletedGame(
                harness = harness,
                result = result,
                expectedCultivationRounds = 6,
                expectedBattleRounds = 3
            )

            assertEquals(4, result.finalScoring.scores.size)
            assertTrue(result.finalScoring.scores.all { it.totalVp >= 0 })
        }
    }

    @Test
    fun `standard 8 Cultivation plus 4 Battle baseline game completes with coherent lifecycle`() {
        val scenario = GameScenario(
            numPlayers = 4,
            roundSetup = GameRoundSetup.standard(),
            seed = 24_680L
        )

        IntegrationGameHarness(scenario).use { harness ->
            val result = harness.runGame()

            WholeGameAssertions.assertCompletedGame(
                harness = harness,
                result = result,
                expectedCultivationRounds = 8,
                expectedBattleRounds = 4
            )

            assertEquals(4, result.finalScoring.scores.size)
            assertTrue(result.finalScoring.scores.all { it.totalVp >= 0 })
        }
    }

    private fun scriptedMiniGamePlayer(): ScriptedDecisionDirector =
        ScriptedDecisionDirector().apply {
            cultivation
                .thenMain(CultivationMainAction.Draw)
                .thenMain(CultivationMainAction.Draw)
                .thenDone()
            buy.thenDone()

            battle
                .thenFirstMain(BattleMainAction.Draw)
                .thenPlacement(StrikeRow.TOP)
                .thenTurn(BattleTurnAction.FinalMain(BattleMainAction.Draw))
                .thenPlacement(StrikeRow.TOP)
        }
}
