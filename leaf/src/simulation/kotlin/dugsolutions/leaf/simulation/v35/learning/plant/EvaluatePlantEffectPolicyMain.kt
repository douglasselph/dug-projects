package dugsolutions.leaf.simulation.v35.learning.plant

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
import dugsolutions.leaf.v35.game.*
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.player.decision.learned.plant.*
import dugsolutions.leaf.v35.round.RoundCardManager
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

private data class CompletedPlantEffectEval(val summary:GameSummary,val entries:List<GameEntry>)

fun main(args:Array<String>){
    val o=PlantEffectEvalOptions.parse(args.toList()); val app=koinApplication{modules(appModules)}
    try{
        val k=app.koin; loadCultivationResearchCards(k.get(),k.get(),k.get(),k.get(),k.get(),k.get())
        val pm=k.get<PlantCardManager>(); val rm=k.get<RoundCardManager>(); val plants=pm.getAllCards().cards; val rounds=rm.getAllCards().cards
        val defaults=FirstGameDefault.PLANT_NAMES.map{requireNotNull(pm.getCard(it))}; val pe=PlantExperimentResearchConfig.resolve(o.plantOverrides,plants); val re=RoundExperimentResearchConfig.resolve(o.roundOverrides,rounds)
        val effectiveCatalog=effectivePlantEffectCatalogCards(plants,pe.values)
        val w=LearnedPlantEffectCatalog.prepare(LearnedPlantEffectWeights.load(o.weights),effectiveCatalog)
        LearnedPlantEffectCatalog.validateCurrentSchema(w, effectiveCatalog)
        val f=k.get<GameFactory>(); val r=k.get<GameRunner>(); val c=Acc(); val l=Acc()
        println("Leaf & Let Die — Learned Plant Effect Held-Out Evaluation")
        println("weights=${o.weights}; matched samples=${o.games}; players=${o.players}; rounds=${o.roundLabel}")
        repeat(o.games){s->
            val seat=affectedSeat(s,o.players); val grove=resolveResearchGroveForSample(o.grovePattern,o.groveSeed,s,pm,defaults,plants,pe.values)
            c.add(run(f,r,grove,List(o.players){PlayerDecisionFactory.humanBaseline()},o,s,seat,"CONTROL",pe,re),seat)
            val lf=learnedPlantEffectFactory(w)
            l.add(run(f,r,grove,List(o.players){if(it==seat)lf else PlayerDecisionFactory.humanBaseline()},o,s,seat,"LEARNED_PLANT_EFFECT",pe,re),seat)
        }
        report("CONTROL — Human Plant Effect Targeting",c,o.games); println(); report("LEARNED — Learned Plant Effect Targeting",l,o.games)
        println(); println("DELTA LEARNED - CONTROL")
        println("  win share=${pct(l.win/o.games-c.win/o.games)} total VP=${fmt(l.total/o.games-c.total/o.games)} Plant VP=${fmt(l.plant/o.games-c.plant/o.games)} Battle VP=${fmt(l.battle/o.games-c.battle/o.games)}")
    }finally{app.close()}
}

private fun run(f:GameFactory,r:GameRunner,grove:List<dugsolutions.leaf.v35.plant.domain.PlantCard>,d:List<PlayerDecisionFactory>,o:PlantEffectEvalOptions,s:Int,seat:Int,v:String,pe:PlantExperimentResearchConfig,re:RoundExperimentResearchConfig):CompletedPlantEffectEval{
    val g=f(GameConfig(selectedPlantCards=grove,playerDecisionFactories=d,roundSetup=o.roundSetup,seed=o.seed+s,strategySeed=o.strategySeed+s,plantValues=pe.values,roundValues=re.values))
    val result=withSimulationFailureDiagnostics(g,SimulationRunContext("evaluate_plant_effect_policy",s,v,seat,o.seed+s,o.strategySeed+s,GrovePlantCode.describe(grove),o.roundLabel)){r.run(g)}
    return CompletedPlantEffectEval(GameSummaryExtractor.extract(g,result),g.chronicle.entries.toList())
}

private class Acc{
    var win=0.0;var total=0.0;var plant=0.0;var battle=0.0;var wisp=0.0;var wounds=0.0;var plantBuys=0L;var dieBuys=0L
    val activations=sortedMapOf<String,Long>(); val decisions=sortedMapOf<String,Long>(); val disagreements=sortedMapOf<String,Long>(); val choices=sortedMapOf<String,Long>()
    var cultActs=0L;var battleActs=0L
    fun add(g:CompletedPlantEffectEval,seat:Int){
        val p=g.summary.players.single{it.seat==seat};win+=p.winShare;total+=p.totalVp;plant+=p.plantVp;battle+=p.battleStrikeVp;wisp+=p.unplayedWispVp;wounds+=p.woundsTaken
        g.entries.filterIsInstance<GameEntry.Purchase>().filter{it.playerId==p.playerId}.forEach{if(it.kind==PurchaseKind.PLANT)plantBuys++ else if(it.kind==PurchaseKind.DIE)dieBuys++}
        g.entries.filterIsInstance<GameEntry.EffectResolved>().filter{it.playerId==p.playerId&&it.sourceKind==EffectSourceKind.PLANT}.forEach{e->activations[e.sourceName]=(activations[e.sourceName]?:0)+1;if(e.phase==ChroniclePhase.CULTIVATION)cultActs++ else battleActs++}
        g.entries.filterIsInstance<GameEntry.PlantEffectDecision>().filter{it.playerId==p.playerId}.forEach{e->
            decisions[e.plantId]=(decisions[e.plantId]?:0)+1
            if(e.referenceChoiceId!=null&&e.referenceChoiceId!=e.selectedChoiceId)disagreements[e.plantId]=(disagreements[e.plantId]?:0)+1
            val key="${e.plantId}|${e.decisionKind}|${e.selectedChoiceId}";choices[key]=(choices[key]?:0)+1
        }
    }
}

private fun report(label:String,a:Acc,n:Int){
    println(label);println("OUTCOME");println("  win share=${pct(a.win/n)} total VP=${fmt(a.total/n)} Plant VP=${fmt(a.plant/n)} Battle VP=${fmt(a.battle/n)} Wisp VP=${fmt(a.wisp/n)} wounds=${fmt(a.wounds/n)}")
    println("ECONOMY");println("  Plant purchases/game=${fmt(a.plantBuys.toDouble()/n)} dice purchases/game=${fmt(a.dieBuys.toDouble()/n)}")
    println("PLANT EXECUTION");println("  Cultivation Plant effects/game=${fmt(a.cultActs.toDouble()/n)} Battle Plant effects/game=${fmt(a.battleActs.toDouble()/n)}")
    (a.activations.keys+a.decisions.keys).toSortedSet().forEach{card->val d=a.decisions[card]?:0;val x=a.disagreements[card]?:0;println("  $card: activations=${a.activations[card]?:0} targeting-decisions=$d human-disagreements=$x disagreement-rate=${pct(if(d==0L)0.0 else x.toDouble()/d)}")}
    println("TARGET / BRANCH DISTRIBUTION")
    a.choices.forEach{(k,v)->println("  $k = $v")}
}
private fun fmt(x:Double)="%.3f".format(x);private fun pct(x:Double)="%.2f%%".format(x*100)

internal data class PlantEffectEvalOptions(
    val games:Int,
    val seed:Long,
    val strategySeed:Long,
    val weights:Path,
    val plantOverrides:Path?,
    val roundOverrides:Path?,
    val grovePattern:String?,
    val groveSeed:Long,
    val players:Int,
    val roundLabel:String
){
    val roundSetup=parseRoundSetup(roundLabel)

    companion object {
        fun parse(args:List<String>):PlantEffectEvalOptions {
            var games=300
            var seed=116000L
            var strategySeed=117000L
            var weights=Paths.get("output/ai/plant-effect-policy-v1-trained.weights")
            var plantOverrides:Path?=null
            var roundOverrides:Path?=null
            var grovePattern:String?=GrovePlantCode.RANDOM_PATTERN
            var groveSeed=118000L
            var players=4
            var rounds="3/2/2"
            var i=0
            fun value(a:String):String = if ('=' in a) a.substringAfter('=') else args[++i]
            while(i<args.size){
                val a=args[i]
                when {
                    a.startsWith("--games") || a.startsWith("--samples") -> games=value(a).toInt()
                    a.startsWith("--seed") -> seed=value(a).toLong()
                    a.startsWith("--strategy-seed") -> strategySeed=value(a).toLong()
                    a.startsWith("--weights") -> weights=Paths.get(value(a))
                    a.startsWith("--plant-overrides") -> plantOverrides=Paths.get(value(a))
                    a.startsWith("--round-overrides") -> roundOverrides=Paths.get(value(a))
                    a=="--random-grove" -> grovePattern=GrovePlantCode.RANDOM_PATTERN
                    a=="--first-game-grove" -> grovePattern=null
                    a.startsWith("--grove-pattern") -> grovePattern=value(a)
                    a.startsWith("--grove-seed") -> groveSeed=value(a).toLong()
                    a.startsWith("--players") -> players=value(a).toInt()
                    a.startsWith("--rounds") -> rounds=value(a)
                    else -> error("Unknown option: $a")
                }
                i++
            }
            require(games>0)
            require(players in 2..4)
            return PlantEffectEvalOptions(games,seed,strategySeed,weights,plantOverrides,roundOverrides,grovePattern,groveSeed,players,rounds)
        }
    }
}
