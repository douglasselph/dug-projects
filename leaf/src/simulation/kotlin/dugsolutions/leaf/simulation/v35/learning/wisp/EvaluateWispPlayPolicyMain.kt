package dugsolutions.leaf.simulation.v35.learning.wisp

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
import dugsolutions.leaf.v35.player.decision.learned.wisp.*
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.wisp.WispCardManager
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

private data class CompletedWispEval(val summary:GameSummary,val entries:List<GameEntry>)
fun main(args:Array<String>){val o=WispEvalOptions.parse(args.toList());val app=koinApplication{modules(appModules)};try{val k=app.koin;loadCultivationResearchCards(k.get(),k.get(),k.get(),k.get(),k.get(),k.get());val pm=k.get<PlantCardManager>();val rm=k.get<RoundCardManager>();val wm=k.get<WispCardManager>();val plants=pm.getAllCards().cards;val rounds=rm.getAllCards().cards;val defaults=FirstGameDefault.PLANT_NAMES.map{requireNotNull(pm.getCard(it))};val pe=PlantExperimentResearchConfig.resolve(o.plantOverrides,plants);val re=RoundExperimentResearchConfig.resolve(o.roundOverrides,rounds);val w=LearnedWispPlayCatalog.prepare(LearnedWispPlayWeights.load(o.weights),wm.getAllCards().cards);val f=k.get<GameFactory>();val r=k.get<GameRunner>();val c=Acc();val l=Acc();println("Leaf & Let Die — Learned Wisp Play Held-Out Evaluation");println("weights=${o.weights}; matched samples=${o.games}; players=${o.players}; rounds=${o.roundLabel}");repeat(o.games){s->val seat=affectedSeat(s,o.players);val grove=resolveResearchGroveForSample(o.grovePattern,o.groveSeed,s,pm,defaults,plants,pe.values);c.add(run(f,r,grove,List(o.players){PlayerDecisionFactory.humanBaseline()},o,s,seat,"CONTROL",pe,re),seat);val lf=learnedWispPlayFactory(w);l.add(run(f,r,grove,List(o.players){if(it==seat)lf else PlayerDecisionFactory.humanBaseline()},o,s,seat,"LEARNED_WISP",pe,re),seat)};report("CONTROL — Human Wisp Play",c,o.games);println();report("LEARNED — Learned Wisp Play",l,o.games);println();println("DELTA LEARNED - CONTROL");println("  win share=${pct(l.win/o.games-c.win/o.games)} total VP=${fmt(l.total/o.games-c.total/o.games)} Battle VP=${fmt(l.battle/o.games-c.battle/o.games)} Wisp VP=${fmt(l.wispVp/o.games-c.wispVp/o.games)}") }finally{app.close()}}
private fun run(f:GameFactory,r:GameRunner,grove:List<dugsolutions.leaf.v35.plant.domain.PlantCard>,d:List<PlayerDecisionFactory>,o:WispEvalOptions,s:Int,seat:Int,v:String,pe:PlantExperimentResearchConfig,re:RoundExperimentResearchConfig):CompletedWispEval{val g=f(GameConfig(selectedPlantCards=grove,playerDecisionFactories=d,roundSetup=o.roundSetup,seed=o.seed+s,strategySeed=o.strategySeed+s,plantValues=pe.values,roundValues=re.values));val result=withSimulationFailureDiagnostics(g,SimulationRunContext("evaluate_wisp_policy",s,v,seat,o.seed+s,o.strategySeed+s,GrovePlantCode.describe(grove),o.roundLabel)){r.run(g)};return CompletedWispEval(GameSummaryExtractor.extract(g,result),g.chronicle.entries.toList())}
private class Acc{var win=0.0;var total=0.0;var plant=0.0;var battle=0.0;var wispVp=0.0;var wounds=0.0;var finalWisps=0L;var gains=0L;var plays=0L;val opp=linkedMapOf<String,Long>();val offered=linkedMapOf<String,Long>();val actual=linkedMapOf<String,Long>();val cult=linkedMapOf<String,Long>();val bat=linkedMapOf<String,Long>();val retained=linkedMapOf<String,Long>();var holds=0L;var decisions=0L
fun add(g:CompletedWispEval,seat:Int){val p=g.summary.players.single{it.seat==seat};win+=p.winShare;total+=p.totalVp;plant+=p.plantVp;battle+=p.battleStrikeVp;wispVp+=p.unplayedWispVp;wounds+=p.woundsTaken;finalWisps+=g.entries.filterIsInstance<GameEntry.RoundCompleted>().lastOrNull()?.playerSummaries?.singleOrNull{it.playerId==p.playerId}?.wispCount?:0;g.entries.filterIsInstance<GameEntry.RollReward>().filter{it.playerId==p.playerId&&it.wispName!=null}.forEach{gains++};g.entries.filterIsInstance<GameEntry.WispAcquired>().filter{it.playerId==p.playerId}.forEach{gains++};g.entries.filterIsInstance<GameEntry.WispPlayDecision>().filter{it.playerId==p.playerId}.forEach{e->decisions++;if(e.selectedWispName==null)holds++;e.legalWispNames.distinct().forEach{opp[it]=(opp[it]?:0)+1};e.selectedWispName?.let{offered[it]=(offered[it]?:0)+1}};g.entries.filterIsInstance<GameEntry.SupportAction>().filter{it.playerId==p.playerId&&it.action==SupportActionKind.WISP}.forEach{e->plays++;val effect=g.entries.filterIsInstance<GameEntry.EffectResolved>().firstOrNull{x->x.playerId==p.playerId&&x.sequence>e.sequence&&x.sourceKind==EffectSourceKind.WISP};val name=effect?.sourceName?:"<unknown>";actual[name]=(actual[name]?:0)+1;if(e.phase==ChroniclePhase.CULTIVATION)cult[name]=(cult[name]?:0)+1 else bat[name]=(bat[name]?:0)+1};g.entries.filterIsInstance<GameEntry.FinalScore>().singleOrNull{it.playerId==p.playerId}?.unplayedWispNames?.forEach{retained[it]=(retained[it]?:0)+1}}
}
private fun report(label:String,a:Acc,n:Int){println(label);println("OUTCOME");println("  win share=${pct(a.win/n)} total VP=${fmt(a.total/n)} Plant VP=${fmt(a.plant/n)} Battle VP=${fmt(a.battle/n)} Wisp VP=${fmt(a.wispVp/n)} wounds=${fmt(a.wounds/n)}");println("WISPS");println("  gains/game=${fmt(a.gains.toDouble()/n)} plays/game=${fmt(a.plays.toDouble()/n)} final/game=${fmt(a.finalWisps.toDouble()/n)}");println("  policy decisions=${a.decisions} HOLD=${a.holds} hold-rate=${pct(if(a.decisions==0L)0.0 else a.holds.toDouble()/a.decisions)}");val names=(a.opp.keys+a.offered.keys+a.actual.keys+a.retained.keys).sorted();names.forEach{name->val o=a.opp[name]?:0;val of=a.offered[name]?:0;val pl=a.actual[name]?:0;println("  $name: opportunities=$o offered=$of offer-rate=${pct(if(o==0L)0.0 else of.toDouble()/o)} plays=$pl cultivation=${a.cult[name]?:0} battle=${a.bat[name]?:0} retained=${a.retained[name]?:0}")}}
private fun fmt(x:Double)="%.3f".format(x);private fun pct(x:Double)="%.2f%%".format(x*100)
internal data class WispEvalOptions(
    val games: Int,
    val seed: Long,
    val strategySeed: Long,
    val weights: Path,
    val plantOverrides: Path?,
    val roundOverrides: Path?,
    val grovePattern: String?,
    val groveSeed: Long,
    val players: Int,
    val roundLabel: String
) {
    val roundSetup = parseRoundSetup(roundLabel)

    companion object {
        fun parse(args: List<String>): WispEvalOptions {
            var games = 300
            var seed = 95000L
            var strategySeed = 105000L
            var weights = Paths.get("output/ai/wisp-play-policy-v1-trained.weights")
            var plantOverrides: Path? = null
            var roundOverrides: Path? = null
            var grovePattern: String? = GrovePlantCode.RANDOM_PATTERN
            var groveSeed = 115000L
            var players = 4
            var rounds = "3/2/2"
            var i = 0
            fun value(arg: String): String = if ('=' in arg) arg.substringAfter('=') else args[++i]
            while (i < args.size) {
                val arg = args[i]
                when {
                    arg.startsWith("--games") || arg.startsWith("--samples") -> games = value(arg).toInt()
                    arg.startsWith("--seed") -> seed = value(arg).toLong()
                    arg.startsWith("--strategy-seed") -> strategySeed = value(arg).toLong()
                    arg.startsWith("--weights") -> weights = Paths.get(value(arg))
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
            require(games > 0)
            require(players in 2..4)
            return WispEvalOptions(games, seed, strategySeed, weights, plantOverrides, roundOverrides, grovePattern, groveSeed, players, rounds)
        }
    }
}
