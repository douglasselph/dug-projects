package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.nio.file.Files

class BattlePacingABBatchTest {
    @Test fun `Task 6 canonical pacing telemetry is read only and sequence complete`() {
        val app = koinApplication { modules(appModules) }
        try {
            val k = app.koin
            loadCultivationResearchCards(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(),
                k.get<WispCardRegistry>(), k.get<WispCardManager>(),
                k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
            val round = k.get<BattleRound>()
            val replay = PairedBattleOpeningReplay(k.get<GameFactory>(), k.get<RoundCoordinator>(), round)
            val config = GameConfig(
                playerDecisionFactories = List(3) { PlayerDecisionFactory.mechanicalControl() },
                roundSetup = GameRoundSetup.standard(), seed = 709911L,
                strategySeed = 77711L, eventBasedDieRolls = true, sharedPercentileDieRolls = true
            )
            val deck = BattlePacingWispDeck.withoutResolve(k.get<WispCardManager>(), 81821L)
            val pair = replay.create(config, 2, deck)
            data class Run(val outcome: String, val windows: List<dugsolutions.leaf.v35.game.round.battle.BattleDecisionWindowResult>,
                           val actionCount: Int)
            fun runA(game: dugsolutions.leaf.v35.game.Game, state: dugsolutions.leaf.v35.battle.BattleState,
                     collect: Boolean): Run {
                val card = game.config.roundValues.cardFor(requireNotNull(game.currentRound))
                val actions = round.executeActions(game, card, state, collectResearchPacing = collect)
                val strikes = round.executeStrikes(game, state)
                val doom = round.executeDoom(game, state)
                round.executeCleanup(game, state)
                val outcome = "${actions.firstMainActions.map { it.action }}|${actions.supportActions.map { it.action }}|" +
                    "${strikes.strikes.map { it.totals to it.winnerIds }}|${doom.count}|${game.players.map { it.vp }}"
                return Run(outcome, actions.decisionWindows,
                    actions.firstMainActions.size + actions.supportActions.size + actions.finalMainActions.size)
            }
            val plain = runA(pair.controlGame, pair.controlBattle, collect = false)
            val instrumented = runA(pair.candidateGame, pair.candidateBattle, collect = true)
            assertEquals(plain.outcome, instrumented.outcome)
            assertTrue(plain.windows.isEmpty())
            assertTrue(instrumented.windows.isNotEmpty())
            assertEquals(instrumented.actionCount,
                instrumented.windows.count { it.chosenMain != null || it.chosenSupport != null })
            assertTrue(instrumented.windows.all { it.beforeChronicleSequence <= it.afterChronicleSequence })
            assertTrue(instrumented.windows.zipWithNext().all { (a, b) -> a.afterChronicleSequence <= b.beforeChronicleSequence })
        } finally { app.close() }
    }

    @Test fun `one mechanical cohort writes complete paired rows and refuses overwrite`() {
        val parent = Files.createTempDirectory("leaf-battle-pacing-cp3-")
        val output = parent.resolve("run")
        val args = arrayOf("--samples", "1", "--players", "2", "--battles", "1",
            "--policy", "mechanical", "--seed", "111001", "--strategy-seed", "221001",
            "--wisp-seed", "331001", "--out", output.toString())
        main(args)
        val pairs = Files.readAllLines(output.resolve("pairs.csv"))
        val strikes = Files.readAllLines(output.resolve("strikes.csv"))
        val turns = Files.readAllLines(output.resolve("opportunities.csv"))
        assertEquals(3, pairs.size) // header + 2 players
        assertEquals(4, strikes.size) // header + 3 Strikes
        assertEquals(7, turns.size) // header + 3 x 2 opportunities
        assertTrue(Files.exists(output.resolve("summary.csv")))
        assertEquals(4, Files.readAllLines(output.resolve("timing_model.csv")).size) // header + 3 assumption profiles
        assertEquals(7, Files.readAllLines(output.resolve("pacing_battles.csv")).size) // A/B x 3 profiles
        assertEquals(13, Files.readAllLines(output.resolve("pacing_players.csv")).size) // A/B x 3 profiles x 2 players
        assertEquals(4, Files.readAllLines(output.resolve("pacing_sensitivity.csv")).size)
        assertEquals(4, Files.readAllLines(output.resolve("pacing_summary.csv")).size)
        val pacingWindows = Files.readAllLines(output.resolve("pacing_windows.csv"))
        assertTrue(pacingWindows.any { it.contains(",A_BASELINE,") })
        assertTrue(pacingWindows.any { it.contains(",B_CANDIDATE,") })
        val battleRows = Files.readAllLines(output.resolve("pacing_battles.csv"))
        val header = battleRows.first().split(',')
        fun value(row: String, name: String) = row.split(',')[header.indexOf(name)]
        val typical = battleRows.drop(1).filter { value(it, "profile") == "typical" }
        assertEquals(2, typical.size)
        val summaryRow = Files.readAllLines(output.resolve("summary.csv"))[1].split(',')
        val a = typical.single { value(it, "ruleSet") == "A_BASELINE" }
        val b = typical.single { value(it, "ruleSet") == "B_CANDIDATE" }
        assertEquals(summaryRow[12], value(a, "supports"))
        assertEquals(summaryRow[14], value(b, "supports"))
        assertTrue(Files.notExists(output.resolve("FAILURE.txt")))
    }

    @Test fun `targeted selection records attempts without changing paired output contract`() {
        val parent = Files.createTempDirectory("leaf-battle-pacing-selection-")
        val output = parent.resolve("run")
        main(arrayOf("--samples", "1", "--players", "2", "--battles", "1",
            "--policy", "mechanical", "--scenario", "resource-rich", "--max-attempts", "100",
            "--out", output.toString()))
        val accepted = Files.readAllLines(output.resolve("opening_selection.csv"))
        val selectionSummary = Files.readAllLines(output.resolve("selection_summary.csv"))
        assertTrue(accepted.size >= 2)
        assertEquals(2, selectionSummary.size)
        val acceptedRows = accepted.drop(1).filter { it.split(",")[6] == "true" }
        assertEquals(1, acceptedRows.size)
        assertTrue(acceptedRows.single().split(",")[12].toInt() >= 8)
        assertEquals(3, Files.readAllLines(output.resolve("pairs.csv")).size)
        assertEquals(4, Files.readAllLines(output.resolve("strikes.csv")).size)
        assertTrue(Files.notExists(output.resolve("FAILURE.txt")))
    }

    @Test fun `human guided pilot records per-row timing plans with two normal Mains`() {
        val parent = Files.createTempDirectory("leaf-battle-pacing-main-plans-")
        val output = parent.resolve("run")
        // Sample zero of the already completed four-player final Battle pilot.
        main(arrayOf("--samples", "1", "--players", "4", "--battles", "3",
            "--policy", "human-guided", "--out", output.toString()))
        val planRows = Files.readAllLines(output.resolve("main_plans.csv"))
        val opportunities = Files.readAllLines(output.resolve("opportunities.csv"))
        assertEquals(13, planRows.size) // header + four players x three Engagements
        assertEquals(13, opportunities.size)
        assertTrue(planRows.drop(1).all { it.contains("ENGAGEMENT_MAIN_PLAN") })
        // Task 2: timing plans must expose actual legal v37 action-score previews.
        assertTrue(planRows.any { it.contains("actionPreviews=") && it.contains("full=") })
        assertTrue(planRows.any { it.contains("RoundEffect") || it.contains("ActivatePlant") || it.contains("Draw") })
        assertTrue(planRows.any { it.contains("reason=MUST_SPEND_TO_USE_BOTH") })
        assertTrue(planRows.any { it.contains("row=MIDDLE") && it.contains("open=MIDDLE|BOTTOM") })
        assertTrue(planRows.any { it.contains("row=BOTTOM") && it.contains("open=BOTTOM") })
        val summary = Files.readAllLines(output.resolve("summary.csv"))[1].split(",")
        assertEquals("8", summary[13]) // candidate normal Main Actions
        assertTrue(summary[14].toInt() > 0) // real v37 tactical Supports, not Mechanical fallback
    }
}
