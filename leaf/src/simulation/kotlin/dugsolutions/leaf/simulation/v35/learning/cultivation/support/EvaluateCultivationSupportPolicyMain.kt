package dugsolutions.leaf.simulation.v35.learning.cultivation.support

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.simulation.v35.learning.cultivation.affectedSeat
import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.v35.chronicle.domain.*
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.*
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.tokens.SharedTokenResource
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

data class CultivationSupportFinalState(
    val water:Int,val mulch:Int,val pendingMulch:Int,val worms:Int,val butterflies:Int,val faceUpButterflies:Int
)
data class CompletedCultivationSupportEvalGame(val summary:GameSummary,val entries:List<GameEntry>,val finalState:CultivationSupportFinalState)

fun main(args:Array<String>){
    val o=CultivationSupportEvalOptions.parse(args.toList()); val app=koinApplication{modules(appModules)}
    try{
        val koin=app.koin; loadCultivationResearchCards(koin.get(),koin.get(),koin.get(),koin.get(),koin.get(),koin.get())
        val plantManager=koin.get<PlantCardManager>();val roundManager=koin.get<RoundCardManager>();val plants=plantManager.getAllCards().cards;val rounds=roundManager.getAllCards().cards
        val defaults=FirstGameDefault.PLANT_NAMES.map{requireNotNull(plantManager.getCard(it))};val plantExp=PlantExperimentResearchConfig.resolve(o.plantOverrides,plants);val roundExp=RoundExperimentResearchConfig.resolve(o.roundOverrides,rounds)
        val weights=LearnedCultivationSupportCatalog.prepare(LearnedCultivationSupportWeights.load(o.weights),plants);LearnedCultivationSupportCatalog.validateCurrentSchema(weights,plants)
        val factory=koin.get<GameFactory>();val runner=koin.get<GameRunner>();val control=EvalAccumulator();val learned=EvalAccumulator()
        println("Leaf & Let Die — Learned Cultivation Support Held-Out Evaluation")
        println("weights=${o.weights}; matched samples=${o.games}; players=${o.players}; rounds=${o.roundLabel}")
        println("Buy/Cultivation Main/Battle Support/Battle Main/Wisp detail/Plant targeting = Human Baseline; intervention changes Cultivation Support only")
        if(plantExp.isActive){println();println(plantExp.render(plants))};if(roundExp.isActive){println();println(roundExp.render(rounds))};println()
        repeat(o.games){sample->
            val seat=affectedSeat(sample,o.players);val grove=resolveResearchGroveForSample(o.grovePattern,o.groveSeed,sample,plantManager,defaults,plants,plantExp.values)
            control.add(runOne(factory,runner,grove,List(o.players){PlayerDecisionFactory.humanBaseline()},o,sample,seat,"CONTROL",plantExp,roundExp),seat)
            val lf=learnedCultivationSupportFactory(weights);learned.add(runOne(factory,runner,grove,List(o.players){if(it==seat)lf else PlayerDecisionFactory.humanBaseline()},o,sample,seat,"LEARNED_CULTIVATION_SUPPORT",plantExp,roundExp),seat)
        }
        printReport("CONTROL — Human Cultivation Support",control,o.games);println();printReport("LEARNED — Learned Cultivation Support",learned,o.games)
        println();println("DELTA LEARNED - CONTROL");println("  win share: ${pp(learned.win/o.games-control.win/o.games)}");println("  total VP: ${fmt(learned.totalVp/o.games-control.totalVp/o.games)}");println("  Battle VP: ${fmt(learned.battleVp/o.games-control.battleVp/o.games)}")
    }finally{app.close()}
}

private fun runOne(factory:GameFactory,runner:GameRunner,grove:List<dugsolutions.leaf.v35.plant.domain.PlantCard>,decisions:List<PlayerDecisionFactory>,o:CultivationSupportEvalOptions,sample:Int,seat:Int,variant:String,plantExp:PlantExperimentResearchConfig,roundExp:RoundExperimentResearchConfig):CompletedCultivationSupportEvalGame{
    val game=factory(GameConfig(selectedPlantCards=grove,playerDecisionFactories=decisions,roundSetup=o.roundSetup,seed=o.seed+sample,strategySeed=o.strategySeed+sample,plantValues=plantExp.values,roundValues=roundExp.values))
    val result=withSimulationFailureDiagnostics(game,SimulationRunContext("evaluate_cultivation_support_policy",sample,variant,seat,o.seed+sample,o.strategySeed+sample,GrovePlantCode.describe(grove),o.roundLabel)){runner.run(game)}
    val player=game.players[seat]
    return CompletedCultivationSupportEvalGame(GameSummaryExtractor.extract(game,result),game.chronicle.entries.toList(),CultivationSupportFinalState(player.tokens.waterCount,player.tokens.mulchCount,player.tokens.pendingMulchCount,player.critters.count(dugsolutions.leaf.v35.tokens.Critter.WORM),player.butterflies.size,player.butterflies.all.count{player.butterflies.isFaceUp(it)}))
}

private class EvalAccumulator{
    var win=0.0;var totalVp=0.0;var plantVp=0.0;var battleVp=0.0;var wispVp=0.0;var wounds=0.0;var plantPurchases=0.0;var diePurchases=0.0;var finalDicePower=0.0
    var decisions=0L;var passes=0L;val opportunities=linkedMapOf<String,Long>();val uses=linkedMapOf<String,Long>();val cultivationMain=linkedMapOf<MainActionKind,Long>()
    var finalWater=0.0;var finalMulch=0.0;var finalWorm=0.0;var finalButterfly=0.0
    var waterRerollDelta=0.0;var waterRerollsMeasured=0L;var refreshedPlants=0L;var refreshedButterflies=0L
    val tokenGains=linkedMapOf<SharedTokenResource,Double>();val tokenUses=linkedMapOf<SharedTokenResource,Double>()
    fun add(g:CompletedCultivationSupportEvalGame,seat:Int){
        val p=g.summary.players.single{it.seat==seat};win+=p.winShare;totalVp+=p.totalVp;plantVp+=p.plantVp;battleVp+=p.battleStrikeVp;wispVp+=p.unplayedWispVp;wounds+=p.woundsTaken;finalDicePower+=p.finalDicePower
        finalWater+=g.finalState.water;finalMulch+=g.finalState.mulch+g.finalState.pendingMulch;finalWorm+=g.finalState.worms;finalButterfly+=g.finalState.butterflies
        g.entries.filterIsInstance<GameEntry.Purchase>().filter{it.playerId==p.playerId}.forEach{if(it.kind==PurchaseKind.PLANT)plantPurchases++ else diePurchases++}
        g.entries.filterIsInstance<GameEntry.MainAction>().filter{it.playerId==p.playerId&&it.phase==ChroniclePhase.CULTIVATION}.forEach{cultivationMain[it.action]=(cultivationMain[it.action]?:0)+1}
        val supportDecisions=g.entries.filterIsInstance<GameEntry.CultivationSupportDecision>().filter{it.playerId==p.playerId}
        supportDecisions.forEach{e->
            decisions++;if(e.selectedActionId==null)passes++
            e.legalActionIds.map(::family).distinct().forEach{opportunities[it]=(opportunities[it]?:0)+1}
            e.selectedActionId?.let{id->
                uses[family(id)]=(uses[family(id)]?:0)+1
                if(family(id)=="WATER_REFRESH"){refreshedPlants+=e.faceDownPlants;refreshedButterflies+=e.spentButterflies}
            }
        }
        val cultSupportEntries=g.entries.filterIsInstance<GameEntry.SupportAction>().filter{it.playerId==p.playerId&&it.phase==ChroniclePhase.CULTIVATION}
        cultSupportEntries.filter{it.action==SupportActionKind.WATER_REROLL}.forEach{support->
            val decision=supportDecisions.lastOrNull{it.sequence<support.sequence&&family(it.selectedActionId?:"")=="WATER_REROLL"}
            val original=decision?.selectedActionId?.substringAfterLast('@')?.toIntOrNull()
            val roll=g.entries.filterIsInstance<GameEntry.DieRolled>().firstOrNull{it.playerId==p.playerId&&it.sequence>support.sequence&&it.hierarchyDepth>support.hierarchyDepth}
            if(original!=null&&roll!=null){waterRerollDelta+=(roll.value-original);waterRerollsMeasured++}
        }
        g.summary.sharedTokenEconomy.forEach{e->tokenGains[e.resource]=(tokenGains[e.resource]?:0.0)+e.successfulGains;tokenUses[e.resource]=(tokenUses[e.resource]?:0.0)+e.spendsOrUses}
    }
}
private fun family(id:String):String=when{":WATER_REROLL:" in id->"WATER_REROLL";id.endsWith(":WATER_REFRESH")->"WATER_REFRESH";":MULCH:" in id->"MULCH";":WORM_FLIP:" in id->"WORM_FLIP";":BUTTERFLY:" in id->"BUTTERFLY";":WISP:" in id->"WISP";else->id}
private val supportFamilies=listOf("WATER_REROLL","WATER_REFRESH","MULCH","WORM_FLIP","BUTTERFLY","WISP")
private fun printReport(label:String,a:EvalAccumulator,n:Int){
    println(label);println("OVERALL");println("  win share=${pp(a.win/n)} total VP=${fmt(a.totalVp/n)} Plant VP=${fmt(a.plantVp/n)} Battle VP=${fmt(a.battleVp/n)} Wisp VP=${fmt(a.wispVp/n)} wounds=${fmt(a.wounds/n)}")
    println("CULTIVATION SUPPORT");println("  decision windows/game=${fmt(a.decisions.toDouble()/n)} PASS/DONE=${fmt(a.passes.toDouble()/n)} (${pp(if(a.decisions==0L)0.0 else a.passes.toDouble()/a.decisions)})")
    supportFamilies.forEach{k->val o=a.opportunities[k]?:0;val u=a.uses[k]?:0;println("  $k: opportunities=$o uses=$u take-rate=${pp(if(o==0L)0.0 else u.toDouble()/o)} uses/game=${fmt(u.toDouble()/n)}")}
    println("  Water reroll realized delta/use=${fmt(if(a.waterRerollsMeasured==0L)0.0 else a.waterRerollDelta/a.waterRerollsMeasured)} measured=${a.waterRerollsMeasured}")
    println("  Water refresh: Plants refreshed/game=${fmt(a.refreshedPlants.toDouble()/n)} Butterflies refreshed/game=${fmt(a.refreshedButterflies.toDouble()/n)}")
    println("ECONOMY");println("  Plants bought/game=${fmt(a.plantPurchases/n)} dice bought/game=${fmt(a.diePurchases/n)} final dice power=${fmt(a.finalDicePower/n)}")
    println("  final affected-player holdings: Water=${fmt(a.finalWater/n)} Mulch=${fmt(a.finalMulch/n)} Worm=${fmt(a.finalWorm/n)} Butterfly=${fmt(a.finalButterfly/n)}")
    println("  whole-table finite-token economy (context for supply pressure):");SharedTokenResource.entries.forEach{r->println("    ${r.name}: gains/game=${fmt((a.tokenGains[r]?:0.0)/n)} uses/game=${fmt((a.tokenUses[r]?:0.0)/n)}")}
    println("CULTIVATION MAIN");MainActionKind.entries.forEach{k->println("  ${k.name}: ${fmt((a.cultivationMain[k]?:0).toDouble()/n)}/game")}
}
private fun fmt(v:Double)="%.3f".format(v);private fun pp(v:Double)="%.2f%%".format(v*100)

internal data class CultivationSupportEvalOptions(val games:Int,val seed:Long,val strategySeed:Long,val weights:Path,val plantOverrides:Path?,val roundOverrides:Path?,val grovePattern:String?,val groveSeed:Long,val players:Int,val roundLabel:String){
    val roundSetup=parseRoundSetup(roundLabel)
    companion object{fun parse(args:List<String>):CultivationSupportEvalOptions{var games=300;var seed=94000L;var strategy=104000L;var weights=Paths.get("output/ai/cultivation-support-policy-v1-trained.weights");var plant:Path?=null;var round:Path?=null;var grove:String?=GrovePlantCode.RANDOM_PATTERN;var groveSeed=114000L;var players=4;var rounds="3/2/2";var i=0;fun value(a:String)=if('=' in a)a.substringAfter('=')else args[++i]
        while(i<args.size){val a=args[i];when{a.startsWith("--games")->games=value(a).toInt();a.startsWith("--seed")->seed=value(a).toLong();a.startsWith("--strategy-seed")->strategy=value(a).toLong();a.startsWith("--weights")->weights=Paths.get(value(a));a.startsWith("--plant-overrides")->plant=Paths.get(value(a));a.startsWith("--round-overrides")->round=Paths.get(value(a));a.startsWith("--grove-pattern")->grove=value(a);a=="--random-grove"->grove=GrovePlantCode.RANDOM_PATTERN;a=="--first-game-grove"->grove=null;a.startsWith("--grove-seed")->groveSeed=value(a).toLong();a.startsWith("--players")->players=value(a).toInt();a.startsWith("--rounds")->rounds=value(a);else->error("Unknown option: $a")};i++};require(players in 2..4);return CultivationSupportEvalOptions(games,seed,strategy,weights,plant,round,grove,groveSeed,players,rounds)}}
}
