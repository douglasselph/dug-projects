package dugsolutions.leaf.simulation.v35.learning.mulch

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
import dugsolutions.leaf.v35.player.decision.learned.mulch.*
import dugsolutions.leaf.v35.round.RoundCardManager
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args:Array<String>){
    val o=MulchTargetTrainOptions.parse(args.toList());val app=koinApplication{modules(appModules)}
    try{
        val k=app.koin;loadCultivationResearchCards(k.get(),k.get(),k.get(),k.get(),k.get(),k.get())
        val pm=k.get<PlantCardManager>();val rm=k.get<RoundCardManager>();val plants=pm.getAllCards().cards;val rounds=rm.getAllCards().cards
        val defaults=FirstGameDefault.PLANT_NAMES.map{requireNotNull(pm.getCard(it))}
        val pe=PlantExperimentResearchConfig.resolve(o.plantOverrides,plants);val re=RoundExperimentResearchConfig.resolve(o.roundOverrides,rounds)
        val groves=List(o.games){s->resolveResearchGroveForSample(o.grovePattern,o.groveSeed,s,pm,defaults,plants,pe.values)}
        val seed=if(Files.exists(o.input)) LearnedMulchTargetWeights.load(o.input) else LearnedMulchTargetWeights.zeros()
        val evo=MulchTargetPolicyEvolution(MulchTargetEvolutionConfig(o.population,o.elites,o.sigma,o.mutations,o.evolutionSeed));var pop=evo.initialPopulation(seed);var best=EvaluatedMulchTargetPolicy(seed,Double.NEGATIVE_INFINITY)
        val factory=k.get<GameFactory>();val runner=k.get<GameRunner>()
        println("Leaf & Let Die — Learned Mulch Target Policy Evolution")
        println("Only WHICH die to Mulch is learned; whether to Mulch and all other policy families = Human Baseline")
        println("players=${o.players} rounds=${o.roundLabel} generations=${o.generations} population=${o.population} games/policy=${o.games}")
        println("plantOverrides=${o.plantOverrides} roundOverrides=${o.roundOverrides}")
        repeat(o.generations){g->
            val evaluated=pop.mapIndexed{ci,w->var wins=0.0;repeat(o.games){s->
                val seat=affectedSeat(s,o.players);val lf=learnedMulchTargetFactory(w);val ds=List(o.players){if(it==seat)lf else PlayerDecisionFactory.humanBaseline()}
                val game=factory(GameConfig(selectedPlantCards=groves[s],playerDecisionFactories=ds,roundSetup=o.roundSetup,seed=o.seed+s,strategySeed=o.strategySeed+s,plantValues=pe.values,roundValues=re.values))
                val result=withSimulationFailureDiagnostics(game,SimulationRunContext("train_mulch_target_g${g+1}_c${ci+1}",s,"LEARNED_MULCH_TARGET",seat,o.seed+s,o.strategySeed+s,GrovePlantCode.describe(groves[s]),o.roundLabel)){runner.run(game)}
                wins+=GameSummaryExtractor.extract(game,result).players.single{it.seat==seat}.winShare
            };EvaluatedMulchTargetPolicy(w,wins/o.games)}.sortedByDescending{it.fitness}
            if(evaluated.first().fitness>best.fitness)best=evaluated.first()
            println("generation=${g+1}/${o.generations} best=${pct(evaluated.first().fitness)} mean=${pct(evaluated.map{it.fitness}.average())} allTime=${pct(best.fitness)}")
            if(g+1<o.generations)pop=evo.nextPopulation(evaluated)
        }
        best.weights.withProvenance(LearnedMulchTargetProvenance("trained",o.roundLabel,o.players,o.generations,o.games,o.population,o.evolutionSeed,o.seed,o.strategySeed,best.fitness)).save(o.output)
        println("Training complete. best=${pct(best.fitness)} output=${o.output}")
    }finally{app.close()}
}
private fun pct(x:Double)="%.2f%%".format(x*100)
internal data class MulchTargetTrainOptions(val generations:Int,val population:Int,val games:Int,val elites:Int,val sigma:Double,val mutations:Int,val evolutionSeed:Long,val seed:Long,val strategySeed:Long,val input:Path,val output:Path,val plantOverrides:Path?,val roundOverrides:Path?,val grovePattern:String?,val groveSeed:Long,val players:Int,val roundLabel:String){
    val roundSetup=parseRoundSetup(roundLabel)
    companion object{fun parse(args:List<String>):MulchTargetTrainOptions{
        var generations=5;var population=8;var games=30;var elites=2;var sigma=.25;var mutations=8;var evolutionSeed=109000L;var seed=119000L;var strategySeed=129000L
        var input=Paths.get("data/ai/4p/mulch-target-policy-v1.weights");var output=Paths.get("output/ai/mulch-target-policy-v1-trained.weights")
        var plantOverrides:Path?=Paths.get("data/research/4p/resync/resync-current.csv");var roundOverrides:Path?=Paths.get("data/research/4p/resync/round-resync-current.csv")
        var grovePattern:String?=GrovePlantCode.RANDOM_PATTERN;var groveSeed=139000L;var players=4;var rounds="3/2/2";var i=0
        fun value(a:String)=if('=' in a)a.substringAfter('=') else args[++i]
        while(i<args.size){val a=args[i];when{
            a.startsWith("--generations")->generations=value(a).toInt();a.startsWith("--population")->population=value(a).toInt();a.startsWith("--games")->games=value(a).toInt();a.startsWith("--elites")->elites=value(a).toInt();a.startsWith("--sigma")->sigma=value(a).toDouble();a.startsWith("--mutations")->mutations=value(a).toInt();a.startsWith("--evolution-seed")->evolutionSeed=value(a).toLong();a.startsWith("--seed")->seed=value(a).toLong();a.startsWith("--strategy-seed")->strategySeed=value(a).toLong();a.startsWith("--input")->input=Paths.get(value(a));a.startsWith("--output")->output=Paths.get(value(a));a.startsWith("--plant-overrides")->plantOverrides=Paths.get(value(a));a.startsWith("--round-overrides")->roundOverrides=Paths.get(value(a));a=="--random-grove"->grovePattern=GrovePlantCode.RANDOM_PATTERN;a=="--first-game-grove"->grovePattern=null;a.startsWith("--grove-pattern")->grovePattern=value(a);a.startsWith("--grove-seed")->groveSeed=value(a).toLong();a.startsWith("--players")->players=value(a).toInt();a.startsWith("--rounds")->rounds=value(a);else->error("Unknown option: $a")};i++}
        require(generations>0&&population>=2&&games>0&&elites in 1 until population&&players in 2..4)
        return MulchTargetTrainOptions(generations,population,games,elites,sigma,mutations,evolutionSeed,seed,strategySeed,input,output,plantOverrides,roundOverrides,grovePattern,groveSeed,players,rounds)
    }}
}
