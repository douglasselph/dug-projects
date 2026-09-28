package dugsolutions.leaf.simulation.v35.experiment.sixwisp

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.intervention.CultivationOpeningDrawFaceIntervention
import dugsolutions.leaf.v35.game.intervention.MechanicalInterventionFactory
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication

private val DEFAULT_PLANTS = listOf(
    "Root_05_02", "Root_07_04", "Root_09_03",
    "Vine_07_01", "Vine_09_01", "Vine_11_04",
    "Flower_11_03", "Flower_14_02", "Flower_17_04"
)

fun main(args: Array<String>) {
    val options = Options.parse(args.toList())
    val application = koinApplication { modules(appModules) }
    try {
        val koin = application.koin
        loadCatalogs(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val plants = DEFAULT_PLANTS.map { name ->
            requireNotNull(plantManager.getCard(name)) { "Unknown Plant card: $name" }
        }
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val control = Accumulator()
        val intervention = Accumulator()
        val factories = List(4) { PlayerDecisionFactory.humanBaseline() }

        repeat(options.games) { sample ->
            val mechanicalSeed = options.baseSeed + sample
            val strategySeed = options.strategyBaseSeed + sample
            val affectedSeat = sample % 4
            val affectedId = PlayerId(affectedSeat + 1)

            val controlSummary = runOne(
                factory, runner, plants, factories, mechanicalSeed, strategySeed,
                MechanicalInterventionFactory.NONE
            )
            control.add(controlSummary, affectedSeat)

            val interventionSummary = runOne(
                factory, runner, plants, factories, mechanicalSeed, strategySeed,
                MechanicalInterventionFactory {
                    CultivationOpeningDrawFaceIntervention(
                        affectedPlayerId = affectedId,
                        firstCultivationRounds = 2,
                        forcedFace = 2
                    )
                }
            )
            intervention.add(interventionSummary, affectedSeat)
        }

        printReport(options, control, intervention)
    } finally {
        application.close()
    }
}

private fun runOne(
    factory: GameFactory,
    runner: GameRunner,
    plants: List<dugsolutions.leaf.v35.plant.domain.PlantCard>,
    decisions: List<PlayerDecisionFactory>,
    mechanicalSeed: Long,
    strategySeed: Long,
    intervention: MechanicalInterventionFactory
): GameSummary {
    val game = factory(
        GameConfig(
            selectedPlantCards = plants,
            playerDecisionFactories = decisions,
            seed = mechanicalSeed,
            strategySeed = strategySeed,
            recordDecisionReasoning = false,
            mechanicalInterventionFactory = intervention
        )
    )
    return GameSummaryExtractor.extract(game, runner.run(game))
}

private class Accumulator {
    var affectedWinShare = 0.0
    var affectedVp = 0L
    val seatWinShare = DoubleArray(4)
    val affectedWinShareBySeat = DoubleArray(4)
    val gamesByAffectedSeat = IntArray(4)

    fun add(summary: GameSummary, affectedSeat: Int) {
        val affected = summary.players.single { it.seat == affectedSeat }
        affectedWinShare += affected.winShare
        affectedVp += affected.totalVp
        gamesByAffectedSeat[affectedSeat]++
        affectedWinShareBySeat[affectedSeat] += affected.winShare
        summary.players.forEach { seatWinShare[it.seat] += it.winShare }
    }
}

private fun printReport(options: Options, control: Accumulator, intervention: Accumulator) {
    fun pct(value: Double) = "%.2f%%".format(value * 100.0)
    fun avg(value: Long) = "%.2f".format(value.toDouble() / options.games)
    val cRate = control.affectedWinShare / options.games
    val iRate = intervention.affectedWinShare / options.games
    val cVp = control.affectedVp.toDouble() / options.games
    val iVp = intervention.affectedVp.toDouble() / options.games

    println("Six-Wisp Opening Experiment")
    println("Matched games per condition: ${options.games}")
    println("Mechanical seeds: ${options.baseSeed}..${options.baseSeed + options.games - 1}")
    println("Strategy seeds:   ${options.strategyBaseSeed}..${options.strategyBaseSeed + options.games - 1}")
    println("Affected role rotates seats 1, 2, 3, 4.")
    println("Intervention: first three opening dice in each of the first two Cultivation rounds are forced to 2 after the natural roll is consumed.")
    println()
    println("Affected Player A")
    println("  Control win share:      ${pct(cRate)} (${"%.2f".format(control.affectedWinShare)} wins)")
    println("  Six-Wisp win share:     ${pct(iRate)} (${"%.2f".format(intervention.affectedWinShare)} wins)")
    println("  Matched win-share delta: ${if (iRate - cRate >= 0) "+" else ""}${pct(iRate - cRate)}")
    println("  Control average VP:     ${avg(control.affectedVp)}")
    println("  Six-Wisp average VP:    ${avg(intervention.affectedVp)}")
    println("  Average VP delta:       ${if (iVp - cVp >= 0) "+" else ""}${"%.2f".format(iVp - cVp)}")
    println()
    println("Affected Player A win share by physical seat")
    for (seat in 0..3) {
        val n = control.gamesByAffectedSeat[seat]
        if (n == 0) {
            println("  Seat ${seat + 1} (n=0): not represented")
        } else {
            val c = control.affectedWinShareBySeat[seat] / n
            val i = intervention.affectedWinShareBySeat[seat] / n
            println("  Seat ${seat + 1} (n=$n): control=${pct(c)}  six-wisp=${pct(i)}  delta=${if (i - c >= 0) "+" else ""}${pct(i - c)}")
        }
    }
    println()
    println("Physical-seat win share across all games")
    for (seat in 0..3) {
        println("  Seat ${seat + 1}: control=${pct(control.seatWinShare[seat] / options.games)}  six-wisp=${pct(intervention.seatWinShare[seat] / options.games)}")
    }
}

private data class Options(val games: Int, val baseSeed: Long, val strategyBaseSeed: Long) {
    companion object {
        fun parse(args: List<String>): Options {
            var games = 2000
            var baseSeed = 12_000L
            var strategySeed = 22_000L
            var positionalGamesSeen = false
            for (arg in args) {
                when {
                    arg.startsWith("--seed=") -> baseSeed = arg.substringAfter('=').toLong()
                    arg.startsWith("--strategy-seed=") -> strategySeed = arg.substringAfter('=').toLong()
                    arg.startsWith("--") -> error("Unknown option: $arg")
                    !positionalGamesSeen -> {
                        games = arg.toInt()
                        positionalGamesSeen = true
                    }
                    else -> error("Unexpected argument: $arg")
                }
            }
            require(games > 0) { "Games must be positive" }
            return Options(games, baseSeed, strategySeed)
        }
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
    plantRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root), CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root))
    plantManager.loadCards(plantRegistry)
    wispRegistry.clear()
    wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
    wispManager.loadCards(wispRegistry)
    roundRegistry.clear()
    roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
    roundManager.loadCards(roundRegistry)
}
