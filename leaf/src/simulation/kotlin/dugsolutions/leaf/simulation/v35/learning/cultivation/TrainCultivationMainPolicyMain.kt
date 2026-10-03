package dugsolutions.leaf.simulation.v35.learning.cultivation

import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainProvenance
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Evolves only high-level Cultivation Main selection; all lower-level choices remain Human Baseline. */
fun main(args: Array<String>) {
    val o = CultivationTrainOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCultivationResearchCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val roundManager = koin.get<RoundCardManager>()
        val allPlants = plantManager.getAllCards().cards
        val allRounds = roundManager.getAllCards().cards
        val defaultGrove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val plantExperiment = PlantExperimentResearchConfig.resolve(o.plantOverridesPath, allPlants)
        val roundExperiment = RoundExperimentResearchConfig.resolve(o.roundOverridesPath, allRounds)
        val groves = List(o.games) { sample ->
            resolveResearchGroveForSample(
                grovePattern = o.grovePattern,
                groveSeed = o.groveSeed,
                sample = sample,
                plantManager = plantManager,
                defaultGrove = defaultGrove,
                allPlants = allPlants,
                plantValues = plantExperiment.values
            )
        }
        val seedWeights = if (Files.exists(o.input)) {
            LearnedCultivationMainWeights.load(o.input)
        } else {
            LearnedCultivationMainWeights.zeros()
        }
        val initial = LearnedCultivationMainCatalog.prepare(
            seedWeights,
            allPlants,
            plantExperiment.effectiveCosts(allPlants)
        )
        val buyWeights = when (o.buyPolicy) {
            "human" -> null
            "learned" -> LearnedBuyCardCatalog.prepare(
                LearnedBuyWeights.load(o.buyWeights),
                allPlants,
                plantExperiment.effectiveCosts(allPlants)
            )
            else -> error("Unsupported Buy policy ${o.buyPolicy}")
        }
        val evolution = CultivationMainPolicyEvolution(
            CultivationMainEvolutionConfig(o.population, o.elites, o.sigma, o.mutations, o.evolutionSeed)
        )
        var population = evolution.initialPopulation(initial)
        var allTime = EvaluatedCultivationMainPolicy(initial, Double.NEGATIVE_INFINITY)
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()

        println("Leaf & Let Die — Learned Cultivation Main Policy Evolution")
        println("input=${o.input} output=${o.output}")
        println("generations=${o.generations} population=${o.population} games/policy=${o.games} players=${o.players}")
        println("rounds=${o.roundLabel}; ${o.groveDescription()}; Buy=${o.buyPolicy}; Battle Main/Support=Human Baseline")
        println("training seeds=${o.seed}..${o.seed + o.games - 1}; strategy seeds=${o.strategySeed}..${o.strategySeed + o.games - 1}")
        println("affected learned role rotates across ${o.players} seats; lower-level effect targets remain Human Baseline")
        if (plantExperiment.isActive) { println(); println(plantExperiment.render(allPlants)) }
        if (roundExperiment.isActive) { println(); println(roundExperiment.render(allRounds)) }
        println()

        repeat(o.generations) { generation ->
            val evaluated = population.mapIndexed { candidate, weights ->
                EvaluatedCultivationMainPolicy(
                    weights,
                    evaluateCultivationCandidate(
                        weights = weights,
                        buyWeights = buyWeights,
                        o = o,
                        factory = factory,
                        runner = runner,
                        groves = groves,
                        plantValues = plantExperiment.values,
                        roundValues = roundExperiment.values,
                        generation = generation,
                        candidate = candidate
                    )
                )
            }.sortedByDescending { it.fitness }
            val best = evaluated.first()
            if (best.fitness > allTime.fitness) {
                allTime = best
                allTime.weights.withProvenance(o.provenance(best.fitness, initial)).save(o.output)
            }
            println(
                "generation=${generation + 1}/${o.generations} " +
                    "best=${pct(best.fitness)} mean=${pct(evaluated.map { it.fitness }.average())} " +
                    "allTime=${pct(allTime.fitness)} saved=${o.output}"
            )
            if (generation + 1 < o.generations) population = evolution.nextPopulation(evaluated)
        }
        allTime.weights.withProvenance(o.provenance(allTime.fitness, initial)).save(o.output)
        println()
        println("Training complete. Best policy written to ${o.output}")
    } finally {
        app.close()
    }
}

private fun evaluateCultivationCandidate(
    weights: LearnedCultivationMainWeights,
    buyWeights: LearnedBuyWeights?,
    o: CultivationTrainOptions,
    factory: GameFactory,
    runner: GameRunner,
    groves: List<List<PlantCard>>,
    plantValues: dugsolutions.leaf.v35.plant.PlantValueResolver,
    roundValues: dugsolutions.leaf.v35.round.RoundValueResolver,
    generation: Int,
    candidate: Int
): Double {
    var wins = 0.0
    repeat(o.games) { sample ->
        val seat = affectedSeat(sample, o.players)
        val mechanicalSeed = o.seed + sample
        val strategySeed = o.strategySeed + sample
        val grove = groves[sample]
        val learnedFactory = learnedCultivationFactory(weights, buyWeights)
        val decisions: List<PlayerDecisionFactory> = List(o.players) { index ->
            if (index == seat) learnedFactory else PlayerDecisionFactory.humanBaseline()
        }
        val game = factory(
            GameConfig(
                selectedPlantCards = grove,
                playerDecisionFactories = decisions,
                roundSetup = o.roundSetup,
                seed = mechanicalSeed,
                strategySeed = strategySeed,
                recordDecisionReasoning = false,
                plantValues = plantValues,
                roundValues = roundValues
            )
        )
        val result = withSimulationFailureDiagnostics(
            game,
            SimulationRunContext(
                experiment = "train_cultivation_main_g${generation + 1}_c${candidate + 1}",
                sample = sample,
                variant = "LEARNED_CULTIVATION_MAIN",
                affectedSeat = seat,
                mechanicalSeed = mechanicalSeed,
                strategySeed = strategySeed,
                grove = GrovePlantCode.describe(grove),
                roundStructure = o.roundLabel
            )
        ) { runner.run(game) }
        wins += GameSummaryExtractor.extract(game, result).players.single { it.seat == seat }.winShare
    }
    return wins / o.games
}

private fun pct(value: Double): String = "%.2f%%".format(value * 100.0)

internal data class CultivationTrainOptions(
    val generations: Int,
    val population: Int,
    val games: Int,
    val elites: Int,
    val sigma: Double,
    val mutations: Int,
    val evolutionSeed: Long,
    val seed: Long,
    val strategySeed: Long,
    val input: Path,
    val output: Path,
    val plantOverridesPath: Path?,
    val roundOverridesPath: Path?,
    val grovePattern: String?,
    val groveSeed: Long,
    val players: Int,
    val roundLabel: String,
    val buyPolicy: String,
    val buyWeights: Path,
    val battleSupportPolicy: String
) {
    val roundSetup = parseRoundSetup(roundLabel)

    fun groveDescription(): String = grovePattern?.let { "Grove pattern=$it (one resolution per sample)" } ?: "Grove=FirstGameDefault"
    fun groveProvenance(): String = grovePattern?.let { "pattern=$it;groveSeed=$groveSeed;perSample=true" } ?: "FirstGameDefault"

    fun provenance(fitness: Double, initial: LearnedCultivationMainWeights) = LearnedCultivationMainProvenance(
        trainingStatus = "trained",
        roundPattern = roundLabel,
        grove = groveProvenance(),
        generations = generations,
        gamesPerPolicy = games,
        population = population,
        playerCount = players,
        mutationSigma = sigma,
        mutationsPerChild = mutations,
        evolutionSeed = evolutionSeed,
        mechanicalSeedStart = seed,
        strategySeedStart = strategySeed,
        fitness = fitness,
        cardManifest = initial.provenance.cardManifest
    )

    companion object {
        fun parse(args: List<String>): CultivationTrainOptions {
            var generations = 5
            var population = 8
            var games = 20
            var elites = 2
            var sigma = 0.25
            var mutations = 8
            var evolutionSeed = 52000L
            var seed = 62000L
            var strategySeed = 72000L
            var input = Paths.get("data/ai/cultivation-main-policy-v1.weights")
            var output = Paths.get("output/ai/cultivation-main-policy-v1-trained.weights")
            var plantOverridesPath: Path? = null
            var roundOverridesPath: Path? = null
            var grovePattern: String? = null
            var groveSeed = 82000L
            var players = 4
            var roundLabel = "3/2/2"
            var buyPolicy = "human"
            var buyWeights = Paths.get("data/ai/frozen-buy-first-game-default-v1.weights")
            var battleSupportPolicy = "human"
            var i = 0
            fun value(arg: String): String = if ('=' in arg) arg.substringAfter('=') else args[++i]
            while (i < args.size) {
                val arg = args[i]
                when {
                    arg.startsWith("--generations") -> generations = value(arg).toInt()
                    arg.startsWith("--population") -> population = value(arg).toInt()
                    arg.startsWith("--games") -> games = value(arg).toInt()
                    arg.startsWith("--elites") -> elites = value(arg).toInt()
                    arg.startsWith("--sigma") -> sigma = value(arg).toDouble()
                    arg.startsWith("--mutations") -> mutations = value(arg).toInt()
                    arg.startsWith("--evolution-seed") -> evolutionSeed = value(arg).toLong()
                    arg.startsWith("--seed") -> seed = value(arg).toLong()
                    arg.startsWith("--strategy-seed") -> strategySeed = value(arg).toLong()
                    arg.startsWith("--input") -> input = Paths.get(value(arg))
                    arg.startsWith("--output") -> output = Paths.get(value(arg))
                    arg.startsWith("--plant-overrides") -> plantOverridesPath = Paths.get(value(arg))
                    arg.startsWith("--round-overrides") -> roundOverridesPath = Paths.get(value(arg))
                    arg.startsWith("--grove-seed") -> groveSeed = value(arg).toLong()
                    arg.startsWith("--players") -> players = value(arg).toInt()
                    arg.startsWith("--rounds") -> roundLabel = value(arg).trim()
                    arg.startsWith("--buy-policy") -> buyPolicy = value(arg).trim().lowercase()
                    arg.startsWith("--buy-weights") -> buyWeights = Paths.get(value(arg))
                    arg.startsWith("--battle-support-policy") -> battleSupportPolicy = value(arg).trim().lowercase()
                    arg.startsWith("--grove") -> grovePattern = GrovePlantCode.validate(value(arg))
                    arg == "--random-grove" -> grovePattern = GrovePlantCode.RANDOM_PATTERN
                    arg == "--help" -> { usage(); kotlin.system.exitProcess(0) }
                    else -> error("Unknown argument: $arg")
                }
                i++
            }
            require(generations > 0)
            require(population >= 2)
            require(games > 0)
            require(elites in 1 until population)
            require(players in 2..4) { "--players must be 2, 3, or 4" }
            require(buyPolicy in setOf("human", "learned")) { "--buy-policy must be human or learned" }
            require(battleSupportPolicy == "human") { "--battle-support-policy currently supports only human; learned policy is a later task" }
            parseRoundSetup(roundLabel)
            return CultivationTrainOptions(
                generations, population, games, elites, sigma, mutations,
                evolutionSeed, seed, strategySeed, input, output,
                plantOverridesPath, roundOverridesPath, grovePattern, groveSeed,
                players, roundLabel, buyPolicy, buyWeights, battleSupportPolicy
            )
        }

        private fun usage() = println(
            "train_cultivation_main_policy [--generations N] [--population N] [--games N] " +
                "[--elites N] [--sigma X] [--mutations N] [--evolution-seed N] [--seed N] " +
                "[--strategy-seed N] [--input PATH] [--output PATH] [--plant-overrides PATH] " +
                "[--round-overrides PATH] [--grove CODE|--random-grove] [--grove-seed N] " +
                "[--players 2|3|4] [--rounds PATTERN] [--buy-policy human|learned] [--buy-weights PATH] [--battle-support-policy human]"
        )
    }
}
