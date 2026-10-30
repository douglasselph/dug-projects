package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.common.CardDataFiles
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PairedBattleOpeningReplayTest {
    @Test
    fun `two three and four player openings pair identically across first middle and final battles`() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val root = CardDataFiles.dataDirectory()
            koin.get<PlantCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root), CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root))
            }.also { koin.get<PlantCardManager>().loadCards(it) }
            koin.get<WispCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
            }.also { koin.get<WispCardManager>().loadCards(it) }
            koin.get<RoundCardRegistry>().apply {
                clear()
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
            }.also { koin.get<RoundCardManager>().loadCards(it) }

            val runner = PairedBattleOpeningReplay(
                gameFactory = koin.get<GameFactory>(),
                roundCoordinator = koin.get<RoundCoordinator>(),
                battleRound = koin.get<BattleRound>()
            )
            for ((players, ordinal) in listOf(2 to 1, 3 to 2, 4 to 3)) {
                val config = GameConfig(
                    playerDecisionFactories = List(players) { PlayerDecisionFactory.mechanicalControl() },
                    roundSetup = GameRoundSetup.standard(),
                    seed = 172901L + players,
                    strategySeed = 3901L,
                    eventBasedDieRolls = true,
                    sharedPercentileDieRolls = true
                )
                val paired = runner.create(config, ordinal)
                assertEquals(paired.fingerprint, BattleOpeningFingerprint.capture(paired.candidateGame, paired.candidateBattle))
                assertEquals(paired.fingerprint, BattleOpeningFingerprint.capture(paired.controlGame, paired.controlBattle))
                assertNotEquals(paired.controlGame, paired.candidateGame)
                assertNotEquals(paired.controlBattle, paired.candidateBattle)
                assertEquals(players * 3 + 1, paired.evidence.size)
                assertTrue(paired.evidence.first().contains("matched=true"))
                println(paired.evidence.joinToString("\n"))
                // A changed real placed die must make the pair check fail; snapshots are frozen.
                val first = paired.candidateBattle.grid.diePlacements.first()
                first.die.adjustTo(if (first.die.value == first.die.sides) 1 else first.die.value + 1)
                assertNotEquals(paired.fingerprint, BattleOpeningFingerprint.capture(paired.candidateGame, paired.candidateBattle))
            }
        } finally {
            app.close()
        }
    }
}
