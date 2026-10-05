package dugsolutions.leaf.simulation.v35.learning.battle.main

import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.simulation.v35.learning.cultivation.affectedSeat
import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.simulation.v35.learning.plant.effectivePlantEffectCatalogCards
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportCatalog
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.battle.main.*
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportCatalog
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectCatalog
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectWeights
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayCatalog
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayWeights
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.wisp.WispCardManager
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
    val o = BattleMainTrainOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCultivationResearchCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val roundManager = koin.get<RoundCardManager>()
        val wispManager = koin.get<WispCardManager>()
        val plants = plantManager.getAllCards().cards
        val rounds = roundManager.getAllCards().cards
        val wisps = wispManager.getAllCards().cards
        val defaults = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val plantExp = PlantExperimentResearchConfig.resolve(o.plantOverrides, plants)
        val roundExp = RoundExperimentResearchConfig.resolve(o.roundOverrides, rounds)
        val effectivePlants = effectivePlantEffectCatalogCards(plants, plantExp.values)
        val groves = List(o.games) { sample ->
            resolveResearchGroveForSample(o.grovePattern, o.groveSeed, sample, plantManager, defaults, plants, plantExp.values)
        }
        val raw = if (Files.exists(o.input)) LearnedBattleMainWeights.load(o.input) else LearnedBattleMainWeights.zeros()
        val seedWeights = LearnedBattleMainCatalog.prepare(raw, effectivePlants)
        val companions = FrozenBattleMainCompanions(
            buy = if (o.buyPolicy == "learned") LearnedBuyCardCatalog.prepare(LearnedBuyWeights.load(o.buyWeights), plants, plantExp.effectiveCosts(plants)) else null,
            cultivationMain = if (o.cultivationMainPolicy == "learned") LearnedCultivationMainCatalog.prepare(LearnedCultivationMainWeights.load(o.cultivationMainWeights), plants, plantExp.effectiveCosts(plants)) else null,
            cultivationSupport = if (o.cultivationSupportPolicy == "learned") LearnedCultivationSupportCatalog.prepare(LearnedCultivationSupportWeights.load(o.cultivationSupportWeights), plants) else null,
            wisp = if (o.wispPolicy == "learned") LearnedWispPlayCatalog.prepare(LearnedWispPlayWeights.load(o.wispWeights), wisps) else null,
            plantEffect = if (o.plantEffectPolicy == "learned") LearnedPlantEffectCatalog.prepare(LearnedPlantEffectWeights.load(o.plantEffectWeights), effectivePlants) else null,
            battleSupport = if (o.battleSupportPolicy == "learned") LearnedBattleSupportCatalog.prepare(LearnedBattleSupportWeights.load(o.battleSupportWeights), plants) else null
        )
        val evolution = BattleMainPolicyEvolution(BattleMainEvolutionConfig(o.population, o.elites, o.sigma, o.mutations, o.evolutionSeed))
        var population = evolution.initialPopulation(seedWeights)
        var allTime = EvaluatedBattleMainPolicy(seedWeights, Double.NEGATIVE_INFINITY)
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()

        println("Leaf & Let Die — Learned Battle Main Policy Evolution")
        println("Only Battle Main weights evolve. Other learned policies, if requested, are frozen.")
        println("generations=${o.generations} population=${o.population} games/policy=${o.games} players=${o.players}; rounds=${o.roundLabel}")
        println("companions: Buy=${o.buyPolicy} CultMain=${o.cultivationMainPolicy} CultSupport=${o.cultivationSupportPolicy} Wisp=${o.wispPolicy} PlantEffect=${o.plantEffectPolicy} BattleSupport=${o.battleSupportPolicy}")
        if (plantExp.isActive) { println(); println(plantExp.render(plants)) }
        if (roundExp.isActive) { println(); println(roundExp.render(rounds)) }
        repeat(o.generations) { gen ->
            val evaluated = population.mapIndexed { candidate, w ->
                var wins = 0.0
                repeat(o.games) { sample ->
                    val seat = affectedSeat(sample, o.players)
                    val learned = learnedBattleMainFactory(w, companions)
                    val decisions = List(o.players) { if (it == seat) learned else PlayerDecisionFactory.humanBaseline() }
                    val game = factory(GameConfig(
                        selectedPlantCards = groves[sample],
                        playerDecisionFactories = decisions,
                        roundSetup = o.roundSetup,
                        seed = o.seed + sample,
                        strategySeed = o.strategySeed + sample,
                        plantValues = plantExp.values,
                        roundValues = roundExp.values
                    ))
                    val result = withSimulationFailureDiagnostics(
                        game,
                        SimulationRunContext("train_battle_main_g${gen + 1}_c${candidate + 1}", sample, "LEARNED_BATTLE_MAIN", seat, o.seed + sample, o.strategySeed + sample, GrovePlantCode.describe(groves[sample]), o.roundLabel)
                    ) { runner.run(game) }
                    wins += GameSummaryExtractor.extract(game, result).players.single { it.seat == seat }.winShare
                }
                EvaluatedBattleMainPolicy(w, wins / o.games)
            }.sortedByDescending { it.fitness }
            if (evaluated.first().fitness > allTime.fitness) allTime = evaluated.first()
            println("generation=${gen + 1}/${o.generations} best=${pct(evaluated.first().fitness)} mean=${pct(evaluated.map { it.fitness }.average())} allTime=${pct(allTime.fitness)}")
            if (gen + 1 < o.generations) population = evolution.nextPopulation(evaluated)
        }
        val provenance = seedWeights.provenance.copy(
            trainingStatus = "trained",
            roundPattern = o.roundLabel,
            grove = o.grovePattern ?: "FirstGameDefault",
            generations = o.generations,
            gamesPerPolicy = o.games,
            population = o.population,
            playerCount = o.players,
            mutationSigma = o.sigma,
            mutationsPerChild = o.mutations,
            evolutionSeed = o.evolutionSeed,
            mechanicalSeedStart = o.seed,
            strategySeedStart = o.strategySeed,
            fitness = allTime.fitness
        )
        allTime.weights.withProvenance(provenance).save(o.output)
        println("Training complete. best=${pct(allTime.fitness)} output=${o.output}")
    } finally { app.close() }
}

private fun pct(v: Double) = "%.2f%%".format(v * 100)

internal data class BattleMainTrainOptions(
    val generations: Int, val population: Int, val games: Int, val elites: Int, val sigma: Double, val mutations: Int,
    val evolutionSeed: Long, val seed: Long, val strategySeed: Long, val input: Path, val output: Path,
    val plantOverrides: Path?, val roundOverrides: Path?, val grovePattern: String?, val groveSeed: Long, val players: Int, val roundLabel: String,
    val buyPolicy: String, val buyWeights: Path,
    val cultivationMainPolicy: String, val cultivationMainWeights: Path,
    val cultivationSupportPolicy: String, val cultivationSupportWeights: Path,
    val wispPolicy: String, val wispWeights: Path,
    val plantEffectPolicy: String, val plantEffectWeights: Path,
    val battleSupportPolicy: String, val battleSupportWeights: Path
) {
    val roundSetup = parseRoundSetup(roundLabel)
    companion object {
        fun parse(args: List<String>): BattleMainTrainOptions {
            var generations = 5; var population = 8; var games = 20; var elites = 2; var sigma = .25; var mutations = 8
            var evolutionSeed = 54000L; var seed = 64000L; var strategySeed = 74000L
            var input = Paths.get("data/ai/battle-main-policy-v1.weights"); var output = Paths.get("output/ai/battle-main-policy-v1-trained.weights")
            var plant: Path? = null; var round: Path? = null; var grove: String? = null; var groveSeed = 84000L; var players = 4; var rounds = "3/2/2"
            var buyPolicy = "human"; var buyWeights = Paths.get("data/ai/buy-policy-v1.weights")
            var cultivationMainPolicy = "human"; var cultivationMainWeights = Paths.get("data/ai/cultivation-main-policy-v1.weights")
            var cultivationSupportPolicy = "human"; var cultivationSupportWeights = Paths.get("data/ai/cultivation-support-policy-v1.weights")
            var wispPolicy = "human"; var wispWeights = Paths.get("data/ai/wisp-play-policy-v1.weights")
            var plantEffectPolicy = "human"; var plantEffectWeights = Paths.get("data/ai/plant-effect-policy-v1.weights")
            var battleSupportPolicy = "human"; var battleSupportWeights = Paths.get("data/ai/battle-support-policy-v1.weights")
            var i = 0
            fun value(a: String) = if ('=' in a) a.substringAfter('=') else args[++i]
            while (i < args.size) {
                val a = args[i]
                when {
                    a.startsWith("--generations") -> generations = value(a).toInt()
                    a.startsWith("--population") -> population = value(a).toInt()
                    a.startsWith("--games") -> games = value(a).toInt()
                    a.startsWith("--elites") -> elites = value(a).toInt()
                    a.startsWith("--sigma") -> sigma = value(a).toDouble()
                    a.startsWith("--mutations") -> mutations = value(a).toInt()
                    a.startsWith("--evolution-seed") -> evolutionSeed = value(a).toLong()
                    a.startsWith("--seed") -> seed = value(a).toLong()
                    a.startsWith("--strategy-seed") -> strategySeed = value(a).toLong()
                    a.startsWith("--input") -> input = Paths.get(value(a))
                    a.startsWith("--output") -> output = Paths.get(value(a))
                    a.startsWith("--plant-overrides") -> plant = Paths.get(value(a))
                    a.startsWith("--round-overrides") -> round = Paths.get(value(a))
                    a.startsWith("--grove-seed") -> groveSeed = value(a).toLong()
                    a.startsWith("--players") -> players = value(a).toInt()
                    a.startsWith("--rounds") -> rounds = value(a)
                    a.startsWith("--grove") -> grove = GrovePlantCode.validate(value(a))
                    a == "--random-grove" -> grove = GrovePlantCode.RANDOM_PATTERN
                    a == "--first-game-grove" -> grove = null
                    a.startsWith("--buy-policy") -> buyPolicy = value(a).lowercase()
                    a.startsWith("--buy-weights") -> buyWeights = Paths.get(value(a))
                    a.startsWith("--cultivation-main-policy") -> cultivationMainPolicy = value(a).lowercase()
                    a.startsWith("--cultivation-main-weights") -> cultivationMainWeights = Paths.get(value(a))
                    a.startsWith("--cultivation-support-policy") -> cultivationSupportPolicy = value(a).lowercase()
                    a.startsWith("--cultivation-support-weights") -> cultivationSupportWeights = Paths.get(value(a))
                    a.startsWith("--wisp-policy") -> wispPolicy = value(a).lowercase()
                    a.startsWith("--wisp-weights") -> wispWeights = Paths.get(value(a))
                    a.startsWith("--plant-effect-policy") -> plantEffectPolicy = value(a).lowercase()
                    a.startsWith("--plant-effect-weights") -> plantEffectWeights = Paths.get(value(a))
                    a.startsWith("--battle-support-policy") -> battleSupportPolicy = value(a).lowercase()
                    a.startsWith("--battle-support-weights") -> battleSupportWeights = Paths.get(value(a))
                    a == "--help" -> { usage(); kotlin.system.exitProcess(0) }
                    else -> error("Unknown option: $a")
                }
                i++
            }
            listOf(buyPolicy, cultivationMainPolicy, cultivationSupportPolicy, wispPolicy, plantEffectPolicy, battleSupportPolicy).forEach {
                require(it in setOf("human", "learned")) { "Companion policy must be human or learned: $it" }
            }
            require(players in 2..4)
            return BattleMainTrainOptions(generations, population, games, elites, sigma, mutations, evolutionSeed, seed, strategySeed, input, output, plant, round, grove, groveSeed, players, rounds,
                buyPolicy, buyWeights, cultivationMainPolicy, cultivationMainWeights, cultivationSupportPolicy, cultivationSupportWeights, wispPolicy, wispWeights, plantEffectPolicy, plantEffectWeights, battleSupportPolicy, battleSupportWeights)
        }
        private fun usage() = println("train_battle_main_policy [training options] [--plant-overrides PATH] [--round-overrides PATH] [--random-grove|--grove CODE] [--players 2|3|4] [--rounds 3/2/2] [--buy-policy human|learned --buy-weights PATH] ... [--battle-support-policy human|learned --battle-support-weights PATH]")
    }
}
