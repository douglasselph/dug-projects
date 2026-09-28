package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.*
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyProvenance
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

/** M3-F2: evolve only Buy selection; every other decision remains Human Baseline. */
fun main(args: Array<String>) {
    val o = TrainOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val grove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val initial = LearnedBuyWeights.load(o.input)
        val evolution = BuyPolicyEvolution(BuyEvolutionConfig(o.population, o.elites, o.sigma, o.mutations, o.evolutionSeed))
        var population = evolution.initialPopulation(initial)
        var allTime = EvaluatedBuyPolicy(initial, Double.NEGATIVE_INFINITY)

        println("M3-F2 — Learned Buy Policy Evolution")
        println("input=${o.input} output=${o.output}")
        println("generations=${o.generations} population=${o.population} games/policy=${o.games}")
        println("training seeds=${o.seed}..${o.seed + o.games - 1}; strategy seeds=${o.strategySeed}..${o.strategySeed + o.games - 1}")
        println("affected learned role rotates across physical seats; opponents=Human Baseline; Grove=FirstGameDefault; rounds=3/2/2")
        println("fitness=affected-role mean win share; identical game/strategy seed cohort for every policy")
        println()

        repeat(o.generations) { generation ->
            val evaluated = population.mapIndexed { candidate, weights ->
                val fitness = evaluate(weights, o, factory, runner, grove, generation, candidate)
                EvaluatedBuyPolicy(weights, fitness)
            }.sortedByDescending { it.fitness }
            val best = evaluated.first()
            if (best.fitness > allTime.fitness) {
                allTime = best
                allTime.weights.withProvenance(LearnedBuyProvenance(
                    trainingStatus = "trained", roundPattern = "3/2/2", grove = "FirstGameDefault",
                    generations = generation + 1, gamesPerPolicy = o.games, population = o.population,
                    mutationSigma = o.sigma, mutationsPerChild = o.mutations, evolutionSeed = o.evolutionSeed, mechanicalSeedStart = o.seed,
                    strategySeedStart = o.strategySeed, fitness = best.fitness
                )).save(o.output) // checkpoint every genuine improvement
            }
            val mean = evaluated.map { it.fitness }.average()
            println("generation=${generation + 1}/${o.generations} best=${pct(best.fitness)} mean=${pct(mean)} allTime=${pct(allTime.fitness)} saved=${o.output}")
            if (generation + 1 < o.generations) population = evolution.nextPopulation(evaluated)
        }
        // Rewrite the final champion with provenance for the complete training run,
        // even when the champion itself was first discovered in an earlier generation.
        allTime.weights.withProvenance(LearnedBuyProvenance(
            trainingStatus = "trained", roundPattern = "3/2/2", grove = "FirstGameDefault",
            generations = o.generations, gamesPerPolicy = o.games, population = o.population,
            mutationSigma = o.sigma, mutationsPerChild = o.mutations, evolutionSeed = o.evolutionSeed,
            mechanicalSeedStart = o.seed, strategySeedStart = o.strategySeed, fitness = allTime.fitness
        )).save(o.output)
        println()
        println("Training complete. Best candidate written to ${o.output}")
        println("The checked-in input policy was NOT overwritten; promote the output deliberately after held-out evaluation.")
    } finally { app.close() }
}

private fun evaluate(
    weights: LearnedBuyWeights,
    o: TrainOptions,
    factory: GameFactory,
    runner: GameRunner,
    grove: List<dugsolutions.leaf.v35.plant.domain.PlantCard>,
    generation: Int,
    candidate: Int
): Double {
    var wins = 0.0
    repeat(o.games) { sample ->
        val seat = sample % 4
        val mechanicalSeed = o.seed + sample
        val strategySeed = o.strategySeed + sample
        val learnedFactory = learnedFactory(weights)
        val decisions = List(4) { if (it == seat) learnedFactory else PlayerDecisionFactory.humanBaseline() }
        val game = factory(GameConfig(
            selectedPlantCards = grove,
            playerDecisionFactories = decisions,
            roundSetup = GameRoundSetup.standard(),
            seed = mechanicalSeed,
            strategySeed = strategySeed,
            recordDecisionReasoning = false
        ))
        val result = withSimulationFailureDiagnostics(game, SimulationRunContext(
            experiment = "train_buy_policy_g${generation + 1}_c${candidate + 1}", sample = sample,
            variant = "LEARNED_BUY", affectedSeat = seat, mechanicalSeed = mechanicalSeed,
            strategySeed = strategySeed, grove = "FirstGameDefault", roundStructure = "3/2/2"
        )) { runner.run(game) }
        wins += GameSummaryExtractor.extract(game, result).players.single { it.seat == seat }.winShare
    }
    return wins / o.games
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

private data class TrainOptions(
    val generations: Int, val population: Int, val games: Int, val elites: Int,
    val sigma: Double, val mutations: Int, val evolutionSeed: Long,
    val seed: Long, val strategySeed: Long, val input: Path, val output: Path
) {
    companion object {
        fun parse(args: List<String>): TrainOptions {
            var generations=5; var population=8; var games=20; var elites=2; var sigma=.25; var mutations=6
            var evolutionSeed=51000L; var seed=61000L; var strategySeed=71000L
            var input=Paths.get("data/ai/buy-policy-v1.weights"); var output=Paths.get("output/ai/buy-policy-v1-trained.weights")
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
                a=="--help" -> { usage(); kotlin.system.exitProcess(0) }
                else -> error("Unknown argument: $a")
            }; i++ }
            require(generations>0); require(population>=2); require(games>0); require(elites in 1 until population)
            return TrainOptions(generations,population,games,elites,sigma,mutations,evolutionSeed,seed,strategySeed,input,output)
        }
        private fun usage() = println("train_buy_policy [--generations N] [--population N] [--games N] [--elites N] [--sigma X] [--mutations N] [--evolution-seed N] [--seed N] [--strategy-seed N] [--input PATH] [--output PATH]")
    }
}
