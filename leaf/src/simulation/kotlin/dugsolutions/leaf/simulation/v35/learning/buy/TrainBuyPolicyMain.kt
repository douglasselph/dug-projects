package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.*
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantValueResolver
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyProvenance
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectCatalog
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectWeights
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportCatalog
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights
import dugsolutions.leaf.simulation.v35.learning.plant.effectivePlantEffectCatalogCards
import dugsolutions.leaf.simulation.v35.learning.interaction.modularFactory
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.RoundValueResolver
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** : evolve only Buy selection; every other decision remains Human Baseline. */
fun main(args: Array<String>) {
    val o = TrainOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val roundManager = koin.get<RoundCardManager>()
        val allPlants = plantManager.getAllCards().cards
        val allRounds = roundManager.getAllCards().cards
        val defaultGrove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val plantExperiment = PlantExperimentResearchConfig.resolve(o.plantOverridesPath, allPlants)
        val roundExperiment = RoundExperimentResearchConfig.resolve(o.roundOverridesPath, allRounds)
        val trainingGroves = resolveTrainingGroves(
            o, plantManager, defaultGrove, allPlants, plantExperiment.values
        )
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val rawInitial = if (Files.exists(o.input)) LearnedBuyWeights.load(o.input) else LearnedBuyWeights.zeros()
        val initial = LearnedBuyCardCatalog.prepare(
            rawInitial,
            allPlants,
            additionalPlantCosts = plantExperiment.effectiveCosts(allPlants)
        )
        val cultivationWeights = when (o.cultivationMainPolicy) {
            "human" -> null
            "learned" -> LearnedCultivationMainCatalog.prepare(
                LearnedCultivationMainWeights.load(o.cultivationMainWeights),
                allPlants,
                plantExperiment.effectiveCosts(allPlants)
            )
            else -> error("Unsupported Cultivation Main policy ${o.cultivationMainPolicy}")
        }
        val effectivePlants = effectivePlantEffectCatalogCards(allPlants, plantExperiment.values)
        val plantEffectWeights = when (o.plantEffectPolicy) {
            "human" -> null
            "learned" -> LearnedPlantEffectCatalog.prepare(
                LearnedPlantEffectWeights.load(o.plantEffectWeights),
                effectivePlants
            )
            else -> error("Unsupported Plant Effect policy ${o.plantEffectPolicy}")
        }
        val battleSupportWeights = when (o.battleSupportPolicy) {
            "human" -> null
            "learned" -> LearnedBattleSupportCatalog.prepare(
                LearnedBattleSupportWeights.load(o.battleSupportWeights),
                effectivePlants
            )
            else -> error("Unsupported Battle Support policy ${o.battleSupportPolicy}")
        }
        val evolution = BuyPolicyEvolution(BuyEvolutionConfig(o.population, o.elites, o.sigma, o.mutations, o.evolutionSeed))
        var population = evolution.initialPopulation(initial)
        var allTime = EvaluatedBuyPolicy(initial, Double.NEGATIVE_INFINITY)

        println(" — Learned Buy Policy Evolution")
        println("input=${o.input} output=${o.output}")
        println("generations=${o.generations} population=${o.population} games/policy=${o.games} players=${o.players}")
        println("training seeds=${o.seed}..${o.seed + o.games - 1}; strategy seeds=${o.strategySeed}..${o.strategySeed + o.games - 1}")
        println("affected learned role rotates across ${o.players} physical seats; opponents=Human Baseline; ${o.groveDescription()}; rounds=${o.roundLabel}")
        if (o.grovePattern != null) {
            println("Grove zeros are resolved once per training sample using grove seeds=${o.groveSeed}..${o.groveSeed + o.games - 1}; every candidate sees the same Grove for the same sample")
            println("resolved training Grove sample 0=${GrovePlantCode.describe(trainingGroves.first())}")
        }
        println("fitness=affected-role mean win share; identical game/strategy seed cohort for every policy")
        println("surrounding context: Cultivation Main=${o.cultivationMainPolicy}; Plant Effect=${o.plantEffectPolicy}; Battle Support=${o.battleSupportPolicy}; all other policies=Human Baseline")
        if (plantExperiment.isActive) {
            println()
            println(plantExperiment.render(allPlants))
        }
        if (roundExperiment.isActive) {
            println()
            println(roundExperiment.render(allRounds))
        }
        println()

        repeat(o.generations) { generation ->
            val evaluated = population.mapIndexed { candidate, weights ->
                val fitness = evaluate(
                    weights, cultivationWeights, plantEffectWeights, battleSupportWeights,
                    o, factory, runner, trainingGroves, plantExperiment.values, roundExperiment.values, generation, candidate
                )
                EvaluatedBuyPolicy(weights, fitness)
            }.sortedByDescending { it.fitness }
            val best = evaluated.first()
            if (best.fitness > allTime.fitness) {
                allTime = best
                allTime.weights.withProvenance(LearnedBuyProvenance(
                    trainingStatus = "trained", roundPattern = o.roundLabel, grove = o.groveProvenance(),
                    generations = generation + 1, gamesPerPolicy = o.games, population = o.population, playerCount = o.players,
                    mutationSigma = o.sigma, mutationsPerChild = o.mutations, evolutionSeed = o.evolutionSeed, mechanicalSeedStart = o.seed,
                    strategySeedStart = o.strategySeed, fitness = best.fitness, cardManifest = initial.provenance.cardManifest
                )).save(o.output) // checkpoint every genuine improvement
            }
            val mean = evaluated.map { it.fitness }.average()
            println("generation=${generation + 1}/${o.generations} best=${pct(best.fitness)} mean=${pct(mean)} allTime=${pct(allTime.fitness)} saved=${o.output}")
            if (generation + 1 < o.generations) population = evolution.nextPopulation(evaluated)
        }
        // Rewrite the final champion with provenance for the complete training run,
        // even when the champion itself was first discovered in an earlier generation.
        allTime.weights.withProvenance(LearnedBuyProvenance(
            trainingStatus = "trained", roundPattern = o.roundLabel, grove = o.groveProvenance(),
            generations = o.generations, gamesPerPolicy = o.games, population = o.population, playerCount = o.players,
            mutationSigma = o.sigma, mutationsPerChild = o.mutations, evolutionSeed = o.evolutionSeed,
            mechanicalSeedStart = o.seed, strategySeedStart = o.strategySeed, fitness = allTime.fitness, cardManifest = initial.provenance.cardManifest
        )).save(o.output)
        println()
        println("Training complete. Best candidate written to ${o.output}")
        println("The checked-in input policy was NOT overwritten; promote the output deliberately after held-out evaluation.")
    } finally { app.close() }
}

private fun evaluate(
    weights: LearnedBuyWeights,
    cultivationWeights: dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights?,
    plantEffectWeights: dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectWeights?,
    battleSupportWeights: dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights?,
    o: TrainOptions,
    factory: GameFactory,
    runner: GameRunner,
    groves: List<List<PlantCard>>,
    plantValues: PlantValueResolver,
    roundValues: RoundValueResolver,
    generation: Int,
    candidate: Int
): Double {
    var wins = 0.0
    repeat(o.games) { sample ->
        val seat = sample % o.players
        val mechanicalSeed = o.seed + sample
        val strategySeed = o.strategySeed + sample
        val grove = groves[sample]
        val groveCode = GrovePlantCode.describe(grove)
        val learnedFactory = modularFactory(
            buyWeights = weights,
            cultivationWeights = cultivationWeights,
            cultivationSupportWeights = null,
            wispWeights = null,
            plantEffectWeights = plantEffectWeights,
            supportWeights = battleSupportWeights,
            battleMainWeights = null
        )
        val decisions = List(o.players) { if (it == seat) learnedFactory else PlayerDecisionFactory.humanBaseline() }
        val game = factory(GameConfig(
            selectedPlantCards = grove,
            playerDecisionFactories = decisions,
            roundSetup = parseRoundSetup(o.roundLabel),
            seed = mechanicalSeed,
            strategySeed = strategySeed,
            recordDecisionReasoning = false,
            plantValues = plantValues,
            roundValues = roundValues
        ))
        val result = withSimulationFailureDiagnostics(game, SimulationRunContext(
            experiment = "train_buy_policy_g${generation + 1}_c${candidate + 1}", sample = sample,
            variant = "LEARNED_BUY", affectedSeat = seat, mechanicalSeed = mechanicalSeed,
            strategySeed = strategySeed, grove = groveCode, roundStructure = o.roundLabel
        )) { runner.run(game) }
        wins += GameSummaryExtractor.extract(game, result).players.single { it.seat == seat }.winShare
    }
    return wins / o.games
}

internal fun resolveTrainingGroves(
    options: TrainOptions,
    plantManager: PlantCardManager,
    defaultGrove: List<PlantCard>,
    allPlants: List<PlantCard>,
    plantValues: PlantValueResolver = PlantValueResolver.CANONICAL,
): List<List<PlantCard>> = List(options.games) { sample ->
    resolveResearchGroveForSample(
        grovePattern = options.grovePattern,
        groveSeed = options.groveSeed,
        sample = sample,
        plantManager = plantManager,
        defaultGrove = defaultGrove,
        allPlants = allPlants,
        plantValues = plantValues,
    )
}

private fun learnedFactory(weights: LearnedBuyWeights): PlayerDecisionFactory = object : PlayerDecisionFactory {
    override fun create() = LearnedBuy.createDirector(weights)
    override fun create(strategyRandomizer: StrategyRandomizer) = LearnedBuy.createDirector(weights, strategyRandomizer)
    override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
        LearnedBuy.createDirector(weights, strategyRandomizer)
}

private fun loadCards(
    plantRegistry: PlantCardRegistry, plantManager: PlantCardManager,
    wispRegistry: WispCardRegistry, wispManager: WispCardManager,
    roundRegistry: RoundCardRegistry, roundManager: RoundCardManager
) {
    val root = CardDataFiles.dataDirectory()
    plantRegistry.clear(); plantRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root), CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root)); plantManager.loadCards(plantRegistry)
    wispRegistry.clear(); wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root)); wispManager.loadCards(wispRegistry)
    roundRegistry.clear(); roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root)); roundManager.loadCards(roundRegistry)
}

private fun pct(x: Double) = "%.2f%%".format(x * 100.0)

internal data class TrainOptions(
    val generations: Int, val population: Int, val games: Int, val elites: Int,
    val sigma: Double, val mutations: Int, val evolutionSeed: Long,
    val seed: Long, val strategySeed: Long, val input: Path, val output: Path,
    val plantOverridesPath: Path?, val roundOverridesPath: Path?, val grovePattern: String?, val groveSeed: Long, val players: Int,
    val roundLabel: String,
    val cultivationMainPolicy: String,
    val cultivationMainWeights: Path,
    val plantEffectPolicy: String,
    val plantEffectWeights: Path,
    val battleSupportPolicy: String,
    val battleSupportWeights: Path
) {
    fun groveDescription(): String = grovePattern?.let { "Grove pattern=$it (one deterministic resolution per training sample)" } ?: "Grove=FirstGameDefault"
    fun groveProvenance(): String = grovePattern?.let { "pattern=$it;groveSeed=$groveSeed;perSample=true" } ?: "FirstGameDefault"

    companion object {
        fun parse(args: List<String>): TrainOptions {
            var generations=5; var population=8; var games=20; var elites=2; var sigma=.25; var mutations=6
            var evolutionSeed=51000L; var seed=61000L; var strategySeed=71000L
            var input=Paths.get("data/ai/4p/buy-policy-v1.weights"); var output=Paths.get("output/ai/buy-policy-v1-trained.weights")
            var plantOverridesPath: Path? = null
            var roundOverridesPath: Path? = null
            var grovePattern: String? = null
            var groveSeed = 81000L
            var players = 4
            var roundLabel = "3/2/2"
            var cultivationMainPolicy = "human"
            var cultivationMainWeights = Paths.get("data/ai/4p/cultivation-main-policy-v1.weights")
            var plantEffectPolicy = "human"
            var plantEffectWeights = Paths.get("data/ai/4p/plant-effect-policy-v1.weights")
            var battleSupportPolicy = "human"
            var battleSupportWeights = Paths.get("data/ai/4p/battle-support-policy-v1.weights")
            var i=0
            fun value(a:String):String = if ('=' in a) a.substringAfter('=') else args[++i]
            while(i<args.size) { val a=args[i]; when {
                a.startsWith("--generations") -> generations=value(a).toInt()
                a.startsWith("--population") -> population=value(a).toInt()
                a.startsWith("--games") -> games=value(a).toInt()
                a.startsWith("--elites") -> elites=value(a).toInt()
                a.startsWith("--sigma") -> sigma=value(a).toDouble()
                a.startsWith("--mutations") -> mutations=value(a).toInt()
                a.startsWith("--evolution-seed") -> evolutionSeed=value(a).toLong()
                a.startsWith("--seed") -> seed=value(a).toLong()
                a.startsWith("--strategy-seed") -> strategySeed=value(a).toLong()
                a.startsWith("--input") -> input=Paths.get(value(a))
                a.startsWith("--output") -> output=Paths.get(value(a))
                a.startsWith("--plant-overrides") -> plantOverridesPath=Paths.get(value(a))
                a.startsWith("--round-overrides") -> roundOverridesPath=Paths.get(value(a))
                a.startsWith("--grove-seed") -> groveSeed=value(a).toLong()
                a.startsWith("--players") -> players=value(a).toInt()
                a.startsWith("--rounds") -> roundLabel=value(a).trim()
                a.startsWith("--cultivation-main-policy") -> cultivationMainPolicy=value(a).trim().lowercase()
                a.startsWith("--cultivation-main-weights") -> cultivationMainWeights=Paths.get(value(a))
                a.startsWith("--plant-effect-policy") -> plantEffectPolicy=value(a).trim().lowercase()
                a.startsWith("--plant-effect-weights") -> plantEffectWeights=Paths.get(value(a))
                a.startsWith("--battle-support-policy") -> battleSupportPolicy=value(a).trim().lowercase()
                a.startsWith("--battle-support-weights") -> battleSupportWeights=Paths.get(value(a))
                a.startsWith("--grove") -> grovePattern=GrovePlantCode.validate(value(a))
                a=="--random-grove" -> grovePattern=GrovePlantCode.RANDOM_PATTERN
                a=="--help" -> { usage(); kotlin.system.exitProcess(0) }
                else -> error("Unknown argument: $a")
            }; i++ }
            require(generations>0); require(population>=2); require(games>0); require(elites in 1 until population); require(players in 2..4) { "--players must be 2, 3, or 4" }
            parseRoundSetup(roundLabel)
            require(cultivationMainPolicy in setOf("human", "learned")) { "--cultivation-main-policy must be human or learned" }
            require(plantEffectPolicy in setOf("human", "learned")) { "--plant-effect-policy must be human or learned" }
            require(battleSupportPolicy in setOf("human", "learned")) { "--battle-support-policy must be human or learned" }
            return TrainOptions(generations,population,games,elites,sigma,mutations,evolutionSeed,seed,strategySeed,input,output,plantOverridesPath,roundOverridesPath,grovePattern,groveSeed,players,roundLabel,cultivationMainPolicy,cultivationMainWeights,plantEffectPolicy,plantEffectWeights,battleSupportPolicy,battleSupportWeights)
        }
        private fun usage() = println("train_buy_policy [--generations N] [--population N] [--games N] [--elites N] [--sigma X] [--mutations N] [--evolution-seed N] [--seed N] [--strategy-seed N] [--input PATH] [--output PATH] [--plant-overrides PATH] [--round-overrides PATH] [--grove CODE|--random-grove] [--grove-seed N] [--players 2|3|4] [--rounds PATTERN] [--cultivation-main-policy human|learned --cultivation-main-weights PATH] [--plant-effect-policy human|learned --plant-effect-weights PATH] [--battle-support-policy human|learned --battle-support-weights PATH]")
    }
}
