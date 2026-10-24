package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import org.koin.dsl.koinApplication

/** Callable legacy batch implementation. Task 5 will connect its sample execution to the orchestrator. */
class BaselineResyncCardBalanceExperiment(
    private val reporter: BaselineResyncCardBalanceReporter = BaselineResyncCardBalanceReporter()
) {
    fun execute(args: Array<String>) = execute(BaselineResyncCardBalanceOptions.parse(args))

    fun execute(config: BaselineResyncCardBalanceOptions) {
        config.output.parentFile?.mkdirs()
        val app = koinApplication { modules(appModules) }
        try {
        val k = app.koin
        val (start, games, players, baseSeed, strategySeed, groveSeed, plantFile, roundFile, output) = config
        loadCatalogs(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(),
            k.get<WispCardRegistry>(), k.get<WispCardManager>(),
            k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
        val plantManager = k.get<PlantCardManager>()
        val roundManager = k.get<RoundCardManager>()
        val allPlants = plantManager.getAllCards().cards
        val researchPlant = PlantExperimentResearchConfig.resolve(plantFile, allPlants)
        val researchRound = RoundExperimentResearchConfig.resolve(roundFile, roundManager.getAllCards().cards)
        val defaults = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val factory = k.get<GameFactory>()
        val runner = k.get<GameRunner>()
        val header = "sample\tplayers\tcard\tgrove_exposed\tlegal_buy_options\taffordable_options\tselected_buy_options\tpurchases\tactivations\tfinal_owned\twinner_share_of_owners\towner_vp_total\tall_player_vp_total"
        output.printWriter().use { writer ->
            writer.println(header)
            repeat(games) { index ->
                val sample = start + index
                val grove = resolveResearchGroveForSample(
                    grovePattern = "000000000", groveSeed = groveSeed, sample = sample,
                    plantManager = plantManager, defaultGrove = defaults,
                    allPlants = allPlants, plantValues = researchPlant.values
                )
                val game = factory(GameConfig(
                    selectedPlantCards = grove,
                    playerDecisionFactories = List(players) { PlayerDecisionFactory.humanBaseline() },
                    roundSetup = GameRoundSetup.standard(),
                    seed = baseSeed + sample,
                    strategySeed = strategySeed + sample,
                    recordDecisionReasoning = false,
                    plantValues = researchPlant.values,
                    roundValues = researchRound.values
                ))
                val result = runner.run(game)
                reporter.writeGame(writer, sample, players, grove, game, result, allPlants)
                if ((index + 1) % 25 == 0) { writer.flush(); println("Baseline resync completed ${index + 1}/$games games; sample=$sample") }
            }
        }
        println("Wrote $games games / ${allPlants.size} Plant identities to ${output.absolutePath}")
        } finally { app.close() }
    }
}
