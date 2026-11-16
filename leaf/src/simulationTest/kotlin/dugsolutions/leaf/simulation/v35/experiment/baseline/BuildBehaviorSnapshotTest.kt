package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.BattleStructure
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildBehaviorSnapshotTest {
    @Test
    fun `each current official Plant maps to its canonical scorer and effect route`() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            loadCatalogs(koin.get<PlantCardRegistry>(), koin.get<PlantCardManager>(),
                koin.get<WispCardRegistry>(), koin.get<WispCardManager>(),
                koin.get<RoundCardRegistry>(), koin.get<RoundCardManager>())
            val rows = BuildBehaviorSnapshot.plantInventory(
                koin.get<PlantCardManager>().getAllCards().cards, HumanBaselineCardScorerRegistry()
            )
            assertEquals(36, rows.size)
            assertTrue(rows.all { it.split('\t').size == 7 })
            assertTrue(rows.any { it.startsWith("Root_05_02\t") })
            // Existing aliases missing from the effect-based scorer lookup:
            // document them; do not mutate scorer behavior in a baseline freeze.
            val unmapped = rows.filter { "\tNO\t" in it }
                .map { it.substringBefore('\t') }.toSet()
            assertEquals(setOf("Root_07_02", "Flower_11_01", "Flower_14_01"), unmapped)
        } finally {
            app.close()
        }
    }

    @Test
    fun `fixed seeds reproduce full legal Build offers choices and resource evidence`() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            loadCatalogs(koin.get<PlantCardRegistry>(), koin.get<PlantCardManager>(),
                koin.get<WispCardRegistry>(), koin.get<WispCardManager>(),
                koin.get<RoundCardRegistry>(), koin.get<RoundCardManager>())
            val grove = FirstGameDefault.PLANT_NAMES.map { id ->
                requireNotNull(koin.get<PlantCardManager>().getCard(id))
            }
            fun run() = koin.get<GameFactory>().invoke(GameConfig(
                selectedPlantCards = grove,
                playerDecisionFactories = List(2) { PlayerDecisionFactory.humanBaseline() },
                roundSetup = GameRoundSetup.standard(),
                battleStructure = BattleStructure.THREE_ENGAGEMENTS,
                seed = 163000L, strategySeed = 263000L,
                recordDecisionReasoning = true
            )).let { game ->
                koin.get<GameRunner>().run(game)
                BuildBehaviorSnapshot.collect(game, 1)
            }
            val first = run()
            val second = run()
            assertEquals(first, second)
            assertTrue(first.decisions.isNotEmpty())
            assertTrue(first.decisions.all { it.split('\t').size == 15 })
            assertTrue(first.finalInventory.size == 2)
        } finally {
            app.close()
        }
    }
}
