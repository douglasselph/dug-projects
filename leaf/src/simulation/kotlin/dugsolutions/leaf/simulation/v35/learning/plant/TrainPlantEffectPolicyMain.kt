package dugsolutions.leaf.simulation.v35.learning.plant

import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.simulation.v35.learning.cultivation.affectedSeat
import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.plant.*
import dugsolutions.leaf.v35.round.RoundCardManager
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args:Array<String>){
    val o=PlantEffectTrainOptions.parse(args.toList());val app=koinApplication{modules(appModules)}
    try{
        val k=app.koin;loadCultivationResearchCards(k.get(),k.get(),k.get(),k.get(),k.get(),k.get())
        val pm=k.get<PlantCardManager>();val rm=k.get<RoundCardManager>();val plants=pm.getAllCards().cards;val rounds=rm.getAllCards().cards
        val defaults=FirstGameDefault.PLANT_NAMES.map{requireNotNull(pm.getCard(it))};val pe=PlantExperimentResearchConfig.resolve(o.plantOverrides,plants);val re=RoundExperimentResearchConfig.resolve(o.roundOverrides,rounds)
        val groves=List(o.games){s->resolveResearchGroveForSample(o.grovePattern,o.groveSeed,s,pm,defaults,plants,pe.values)}
        val raw=if(Files.exists(o.input))LearnedPlantEffectWeights.load(o.input) else LearnedPlantEffectWeights.zeros();val seed=LearnedPlantEffectCatalog.prepare(raw,effectivePlantEffectCatalogCards(plants,pe.values))
        val buyWeights = when (o.buyPolicy) {
            "human" -> null
            "learned" -> LearnedBuyCardCatalog.prepare(
                LearnedBuyWeights.load(o.buyWeights),
                plants,
                pe.effectiveCosts(plants)
            )
            else -> error("Unsupported Buy policy ${o.buyPolicy}")
        }
        val evolution=PlantEffectPolicyEvolution(PlantEffectEvolutionConfig(o.population,o.elites,o.sigma,o.mutations,o.evolutionSeed));var pop=evolution.initialPopulation(seed);var best=EvaluatedPlantEffectPolicy(seed,Double.NEGATIVE_INFINITY)
        val factory=k.get<GameFactory>();val runner=k.get<GameRunner>()
        println("Leaf & Let Die — Learned Plant Effect Policy Evolution")
        println("Only Plant effect execution/targeting is trained; Buy=${o.buyPolicy}; Cultivation Main/Cultivation Support/Wisp/Battle Support/Battle Main = Human Baseline")
        println("generations=${o.generations} population=${o.population} games/policy=${o.games} players=${o.players}; rounds=${o.roundLabel}")
        repeat(o.generations){g->
            val eval=pop.mapIndexed{ci,w->var wins=0.0;repeat(o.games){s->val seat=affectedSeat(s,o.players);val lf=learnedPlantEffectFactory(w, buyWeights);val ds=List(o.players){if(it==seat)lf else PlayerDecisionFactory.humanBaseline()};val game=factory(GameConfig(selectedPlantCards=groves[s],playerDecisionFactories=ds,roundSetup=o.roundSetup,seed=o.seed+s,strategySeed=o.strategySeed+s,plantValues=pe.values,roundValues=re.values));val result=withSimulationFailureDiagnostics(game,SimulationRunContext("train_plant_effect_g${g+1}_c${ci+1}",s,"LEARNED_PLANT_EFFECT",seat,o.seed+s,o.strategySeed+s,GrovePlantCode.describe(groves[s]),o.roundLabel)){runner.run(game)};wins+=GameSummaryExtractor.extract(game,result).players.single{it.seat==seat}.winShare};EvaluatedPlantEffectPolicy(w,wins/o.games)}.sortedByDescending{it.fitness};if(eval.first().fitness>best.fitness)best=eval.first();println("generation=${g+1}/${o.generations} best=${pct(eval.first().fitness)} mean=${pct(eval.map{it.fitness}.average())}");pop=evolution.nextPopulation(eval)
        }
        best.weights.withProvenance(best.weights.provenance.copy(trainingStatus="trained",roundPattern=o.roundLabel,grove=o.grovePattern?:"FirstGameDefault",generations=o.generations,gamesPerPolicy=o.games,population=o.population,playerCount=o.players,evolutionSeed=o.evolutionSeed,mechanicalSeedStart=o.seed,strategySeedStart=o.strategySeed,fitness=best.fitness,roundConfiguration=o.roundOverrides?.toString()?:"canonical")).save(o.output)
        println("Training complete. best=${pct(best.fitness)} output=${o.output}")
    }finally{app.close()}
}
private fun pct(x:Double)="%.2f%%".format(x*100)
internal data class PlantEffectTrainOptions(
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
    val plantOverrides: Path?,
    val roundOverrides: Path?,
    val grovePattern: String?,
    val groveSeed: Long,
    val players: Int,
    val roundLabel: String,
    val buyPolicy: String,
    val buyWeights: Path
) {
    val roundSetup = parseRoundSetup(roundLabel)

    companion object {
        fun parse(args: List<String>): PlantEffectTrainOptions {
            var generations = 5
            var population = 8
            var games = 20
            var elites = 2
            var sigma = .25
            var mutations = 8
            var evolutionSeed = 96000L
            var seed = 97000L
            var strategySeed = 98000L
            var input = Paths.get("data/ai/4p/plant-effect-policy-v1.weights")
            var output = Paths.get("output/ai/plant-effect-policy-v1-trained.weights")
            var plantOverrides: Path? = null
            var roundOverrides: Path? = null
            var grovePattern: String? = GrovePlantCode.RANDOM_PATTERN
            var groveSeed = 99000L
            var players = 4
            var rounds = "3/2/2"
            var buyPolicy = "human"
            var buyWeights = Paths.get("data/ai/4p/buy-policy-v1.weights")
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
                    arg.startsWith("--plant-overrides") -> plantOverrides = Paths.get(value(arg))
                    arg.startsWith("--round-overrides") -> roundOverrides = Paths.get(value(arg))
                    arg == "--random-grove" -> grovePattern = GrovePlantCode.RANDOM_PATTERN
                    arg == "--first-game-grove" -> grovePattern = null
                    arg.startsWith("--grove-pattern") -> grovePattern = value(arg)
                    arg.startsWith("--grove-seed") -> groveSeed = value(arg).toLong()
                    arg.startsWith("--players") -> players = value(arg).toInt()
                    arg.startsWith("--rounds") -> rounds = value(arg)
                    arg.startsWith("--buy-policy") -> buyPolicy = value(arg).trim().lowercase()
                    arg.startsWith("--buy-weights") -> buyWeights = Paths.get(value(arg))
                    else -> error("Unknown option: $arg")
                }
                i++
            }
            require(generations > 0)
            require(population >= 2)
            require(games > 0)
            require(elites in 1 until population)
            require(players in 2..4)
            require(buyPolicy in setOf("human", "learned")) { "--buy-policy must be human or learned" }
            return PlantEffectTrainOptions(generations, population, games, elites, sigma, mutations, evolutionSeed, seed, strategySeed, input, output, plantOverrides, roundOverrides, grovePattern, groveSeed, players, rounds, buyPolicy, buyWeights)
        }
    }
}
