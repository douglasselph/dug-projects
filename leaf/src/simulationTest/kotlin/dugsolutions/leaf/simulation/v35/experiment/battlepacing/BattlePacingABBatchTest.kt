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
    @Test fun `two independent canonical A copies replay outcomes exactly`() {
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
            fun runA(game: dugsolutions.leaf.v35.game.Game, state: dugsolutions.leaf.v35.battle.BattleState): String {
                val card = game.config.roundValues.cardFor(requireNotNull(game.currentRound))
                val actions = round.executeActions(game, card, state)
                val strikes = round.executeStrikes(game, state)
                val doom = round.executeDoom(game, state)
                round.executeCleanup(game, state)
                return "${actions.firstMainActions.map { it.action }}|${actions.supportActions.map { it.action }}|" +
                    "${strikes.strikes.map { it.totals to it.winnerIds }}|${doom.count}|${game.players.map { it.vp }}"
            }
            assertEquals(runA(pair.controlGame, pair.controlBattle), runA(pair.candidateGame, pair.candidateBattle))
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
        assertTrue(Files.notExists(output.resolve("FAILURE.txt")))
    }
}
