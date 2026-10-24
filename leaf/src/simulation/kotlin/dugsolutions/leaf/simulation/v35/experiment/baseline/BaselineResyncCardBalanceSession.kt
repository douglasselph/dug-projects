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

/** One experiment-wide lifetime: catalog loading, research overrides and Koin are reused. */
class BaselineResyncCardBalanceSession(
    private val config: BaselineResyncCardBalanceOptions,
    private val reporter: BaselineResyncCardBalanceReporter = BaselineResyncCardBalanceReporter()
) : AutoCloseable {
    private val app = koinApplication { modules(appModules) }
    private val k = app.koin
    private val plantManager: PlantCardManager
    private val allPlants: List<dugsolutions.leaf.v35.plant.domain.PlantCard>
    private val researchPlant: PlantExperimentResearchConfig
    private val researchRound: RoundExperimentResearchConfig
    private val defaults: List<dugsolutions.leaf.v35.plant.domain.PlantCard>
    private val factory: GameFactory
    private val runner: GameRunner

    init {
        try {
            loadCatalogs(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(),
                k.get<WispCardRegistry>(), k.get<WispCardManager>(),
                k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
            plantManager = k.get()
            val roundManager = k.get<RoundCardManager>()
            allPlants = plantManager.getAllCards().cards
            researchPlant = PlantExperimentResearchConfig.resolve(config.plantFile, allPlants)
            researchRound = RoundExperimentResearchConfig.resolve(config.roundFile, roundManager.getAllCards().cards)
            defaults = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
            factory = k.get()
            runner = k.get()
        } catch (failure: Throwable) {
            app.close()
            throw failure
        }
    }

    val plantCount: Int get() = allPlants.size

    fun runSample(writer: java.io.PrintWriter, sample: Int) {
        val grove = resolveResearchGroveForSample(
            grovePattern = "000000000", groveSeed = config.groveSeed, sample = sample,
            plantManager = plantManager, defaultGrove = defaults,
            allPlants = allPlants, plantValues = researchPlant.values
        )
        val game = factory(GameConfig(
            selectedPlantCards = grove,
            playerDecisionFactories = List(config.players) { PlayerDecisionFactory.humanBaseline() },
            roundSetup = GameRoundSetup.standard(),
            seed = config.baseSeed + sample,
            strategySeed = config.strategySeed + sample,
            recordDecisionReasoning = false,
            plantValues = researchPlant.values,
            roundValues = researchRound.values
        ))
        val result = runner.run(game)
        reporter.writeGame(writer, sample, config.players, grove, game, result, allPlants)
    }

    override fun close() = app.close()

    companion object {
        const val HEADER = "sample\tplayers\tcard\tgrove_exposed\tlegal_buy_options\taffordable_options\tselected_buy_options\tpurchases\tactivations\tfinal_owned\twinner_share_of_owners\towner_vp_total\tall_player_vp_total"
    }
}
