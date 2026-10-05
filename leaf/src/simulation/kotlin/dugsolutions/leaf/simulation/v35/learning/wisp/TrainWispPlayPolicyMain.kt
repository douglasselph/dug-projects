package dugsolutions.leaf.simulation.v35.learning.wisp

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
import dugsolutions.leaf.v35.player.decision.learned.wisp.*
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.wisp.WispCardManager
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args:Array<String>){
    val o=WispTrainOptions.parse(args.toList());val app=koinApplication{modules(appModules)}
    try{
        val k=app.koin;loadCultivationResearchCards(k.get(),k.get(),k.get(),k.get(),k.get(),k.get())
        val pm=k.get<PlantCardManager>();val rm=k.get<RoundCardManager>();val wm=k.get<WispCardManager>();val plants=pm.getAllCards().cards;val rounds=rm.getAllCards().cards;val wisps=wm.getAllCards().cards
        val defaults=FirstGameDefault.PLANT_NAMES.map{requireNotNull(pm.getCard(it))};val pe=PlantExperimentResearchConfig.resolve(o.plantOverrides,plants);val re=RoundExperimentResearchConfig.resolve(o.roundOverrides,rounds)
        val groves=List(o.games){s->resolveResearchGroveForSample(o.grovePattern,o.groveSeed,s,pm,defaults,plants,pe.values)}
        val raw=if(Files.exists(o.input))LearnedWispPlayWeights.load(o.input) else LearnedWispPlayWeights.zeros();val seed=LearnedWispPlayCatalog.prepare(raw,wisps)
        val evolution=WispPlayPolicyEvolution(WispPlayEvolutionConfig(o.population,o.elites,o.sigma,o.mutations,o.evolutionSeed));var pop=evolution.initialPopulation(seed);var best=EvaluatedWispPlayPolicy(seed,Double.NEGATIVE_INFINITY)
        val factory=k.get<GameFactory>();val runner=k.get<GameRunner>()
        println("Leaf & Let Die — Learned Wisp Play Policy Evolution")
        println("Only Wisp Play is learned; Buy/Cultivation Main/Cultivation Support/Battle Support/Battle Main/Plant targeting = Human Baseline")
        println("generations=${o.generations} population=${o.population} games/policy=${o.games} players=${o.players}; rounds=${o.roundLabel}")
        repeat(o.generations){g->
            val eval=pop.mapIndexed{ci,w->var wins=0.0;repeat(o.games){s->val seat=affectedSeat(s,o.players);val lf=learnedWispPlayFactory(w);val ds=List(o.players){if(it==seat)lf else PlayerDecisionFactory.humanBaseline()};val game=factory(GameConfig(selectedPlantCards=groves[s],playerDecisionFactories=ds,roundSetup=o.roundSetup,seed=o.seed+s,strategySeed=o.strategySeed+s,plantValues=pe.values,roundValues=re.values));val result=withSimulationFailureDiagnostics(game,SimulationRunContext("train_wisp_g${g+1}_c${ci+1}",s,"LEARNED_WISP",seat,o.seed+s,o.strategySeed+s,GrovePlantCode.describe(groves[s]),o.roundLabel)){runner.run(game)};wins+=GameSummaryExtractor.extract(game,result).players.single{it.seat==seat}.winShare};EvaluatedWispPlayPolicy(w,wins/o.games)}.sortedByDescending{it.fitness};if(eval.first().fitness>best.fitness)best=eval.first();println("generation=${g+1}/${o.generations} best=${pct(eval.first().fitness)} mean=${pct(eval.map{it.fitness}.average())}");pop=evolution.nextPopulation(eval)
        }
        best.weights.withProvenance(best.weights.provenance.copy(trainingStatus="trained",roundPattern=o.roundLabel,grove=o.grovePattern?:"FirstGameDefault",generations=o.generations,gamesPerPolicy=o.games,population=o.population,playerCount=o.players,evolutionSeed=o.evolutionSeed,mechanicalSeedStart=o.seed,strategySeedStart=o.strategySeed,fitness=best.fitness,roundConfiguration=o.roundOverrides?.toString()?:"canonical")).save(o.output)
        println("Training complete. best=${pct(best.fitness)} output=${o.output}")
    }finally{app.close()}
}
private fun pct(x:Double)="%.2f%%".format(x*100)
internal data class WispTrainOptions(
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
    val roundLabel: String
) {
    val roundSetup = parseRoundSetup(roundLabel)

    companion object {
        fun parse(args: List<String>): WispTrainOptions {
            var generations = 5
            var population = 8
            var games = 20
            var elites = 2
            var sigma = .25
            var mutations = 8
            var evolutionSeed = 55000L
            var seed = 65000L
            var strategySeed = 75000L
            var input = Paths.get("data/ai/wisp-play-policy-v1.weights")
            var output = Paths.get("output/ai/wisp-play-policy-v1-trained.weights")
            var plantOverrides: Path? = null
            var roundOverrides: Path? = null
            var grovePattern: String? = GrovePlantCode.RANDOM_PATTERN
            var groveSeed = 85000L
            var players = 4
            var rounds = "3/2/2"
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
                    else -> error("Unknown option: $arg")
                }
                i++
            }
            require(generations > 0)
            require(population >= 2)
            require(games > 0)
            require(elites in 1 until population)
            require(players in 2..4)
            return WispTrainOptions(generations, population, games, elites, sigma, mutations, evolutionSeed, seed, strategySeed, input, output, plantOverrides, roundOverrides, grovePattern, groveSeed, players, rounds)
        }
    }
}
