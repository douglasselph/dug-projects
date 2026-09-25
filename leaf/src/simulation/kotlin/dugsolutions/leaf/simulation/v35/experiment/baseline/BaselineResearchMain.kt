package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.GameSummaryBatchRunner
import dugsolutions.leaf.simulation.v35.experiment.Matchup
import dugsolutions.leaf.simulation.v35.strategy.StrategyProfile
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication

/** Manual research entry point. Automated correctness remains in simulationTest. */
fun main(args: Array<String>) {
    val options = BaselineResearchOptions.parse(args.toList())
    val application = koinApplication { modules(appModules) }
    try {
        val koin = application.koin
        loadCatalogs(
            koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get()
        )
        val plantManager = koin.get<PlantCardManager>()
        val plants = options.plantNames.map { name ->
            requireNotNull(plantManager.getCard(name)) { "Unknown Plant card: $name" }
        }
        val spec = BaselineCalibrationSpec(
            selectedPlantCards = plants,
            games = options.games,
            baseSeed = options.baseSeed,
            strategyBaseSeed = options.strategyBaseSeed,
            checkpoints = options.checkpoints
        )
        val runner = GameSummaryBatchRunner(
            gameFactory = koin.get<GameFactory>(),
            gameRunner = koin.get<GameRunner>(),
            selectedPlantCards = plants
        )
        val matchup = Matchup("Human Baseline", List(4) { StrategyProfile.humanBaseline() })
        val batch = runner.run(matchup, spec.experimentConfig())

        when (options.mode) {
            BaselineResearchMode.DIAGNOSTIC -> {
                println("Grove: ${spec.groveFingerprint}")
                println(BaselineRandomnessDiagnosticRenderer.render(BaselineRandomnessDiagnostic.from(batch)))
            }
            BaselineResearchMode.CALIBRATION -> {
                println(BaselineCalibrationReportRenderer.render(BaselineCalibrationAggregator.aggregate(spec, batch)))
            }
        }
    } finally {
        application.close()
    }
}

internal enum class BaselineResearchMode { DIAGNOSTIC, CALIBRATION }

internal data class BaselineResearchOptions(
    val mode: BaselineResearchMode,
    val games: Int,
    val baseSeed: Long,
    val strategyBaseSeed: Long,
    val checkpoints: List<Int>,
    val plantNames: List<String>
) {
    companion object {
        val DEFAULT_PLANT_NAMES = listOf(
            "Root_05_02", "Root_07_04", "Root_09_03",
            "Vine_07_01", "Vine_09_01", "Vine_11_04",
            "Flower_11_03", "Flower_14_02", "Flower_17_04"
        )

        fun parse(args: List<String>): BaselineResearchOptions {
            require(args.isNotEmpty()) { "First argument must be diagnostic or calibration" }
            val mode = when (args.first().lowercase()) {
                "diagnostic" -> BaselineResearchMode.DIAGNOSTIC
                "calibration" -> BaselineResearchMode.CALIBRATION
                else -> error("Unknown baseline research mode: ${args.first()}")
            }
            val values = args.drop(1).associate { arg ->
                require(arg.startsWith("--") && '=' in arg) { "Expected --name=value, got: $arg" }
                val (key, value) = arg.removePrefix("--").split('=', limit = 2)
                key to value
            }
            val games = values["games"]?.toInt() ?: if (mode == BaselineResearchMode.DIAGNOSTIC) 12 else 2000
            val baseSeed = values["base-seed"]?.toLong() ?: 12_000L
            val strategyBaseSeed = values["strategy-seed"]?.toLong() ?: 22_000L
            val checkpoints = values["checkpoints"]?.csvInts()
                ?: if (mode == BaselineResearchMode.DIAGNOSTIC) listOf(games)
                else listOf(100, 250, 500, 1000, 2000).filter { it <= games }.let { if (it.lastOrNull() == games) it else it + games }
            val plantNames = values["plants"]?.csvStrings() ?: DEFAULT_PLANT_NAMES
            return BaselineResearchOptions(mode, games, baseSeed, strategyBaseSeed, checkpoints, plantNames)
        }

        private fun String.csvInts(): List<Int> = csvStrings().map(String::toInt)
        private fun String.csvStrings(): List<String> = split(',').map(String::trim).filter(String::isNotEmpty)
    }
}

private fun loadCatalogs(
    plantRegistry: PlantCardRegistry,
    plantManager: PlantCardManager,
    wispRegistry: WispCardRegistry,
    wispManager: WispCardManager,
    roundRegistry: RoundCardRegistry,
    roundManager: RoundCardManager
) {
    val root = CardDataFiles.dataDirectory()
    plantRegistry.clear()
    plantRegistry.loadFromCsv(
        CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root),
        CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root)
    )
    plantManager.loadCards(plantRegistry)
    wispRegistry.clear()
    wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
    wispManager.loadCards(wispRegistry)
    roundRegistry.clear()
    roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
    roundManager.loadCards(roundRegistry)
}
