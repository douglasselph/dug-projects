package dugsolutions.leaf.simulation.v35.learning.battle

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
import dugsolutions.leaf.v35.chronicle.domain.BattleMainStage
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
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
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.tokens.SharedTokenResource
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

data class CompletedBattleSupportEvalGame(val summary: GameSummary, val entries: List<GameEntry>)

fun main(args: Array<String>) {
    val o = BattleSupportEvalOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCultivationResearchCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>(); val roundManager = koin.get<RoundCardManager>()
        val plants = plantManager.getAllCards().cards; val rounds = roundManager.getAllCards().cards
        val defaults = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val plantExp = PlantExperimentResearchConfig.resolve(o.plantOverrides, plants)
        val roundExp = RoundExperimentResearchConfig.resolve(o.roundOverrides, rounds)
        val weights = LearnedBattleSupportCatalog.prepare(LearnedBattleSupportWeights.load(o.weights), plants)
        LearnedBattleSupportCatalog.validateCurrentSchema(weights, plants)
        val factory = koin.get<GameFactory>(); val runner = koin.get<GameRunner>()
        val control = EvalAccumulator(); val learned = EvalAccumulator()

        println("Leaf & Let Die — Learned Battle Support Held-Out Evaluation")
        println("weights=${o.weights}; matched samples=${o.games}; players=${o.players}; rounds=${o.roundLabel}")
        println("Buy/Cultivation Main/Battle Main/lower-level targets = Human Baseline; intervention changes Battle Support only")
        if (plantExp.isActive) { println(); println(plantExp.render(plants)) }
        if (roundExp.isActive) { println(); println(roundExp.render(rounds)) }
        println()

        repeat(o.games) { sample ->
            val seat = affectedSeat(sample, o.players)
            val grove = resolveResearchGroveForSample(o.grovePattern, o.groveSeed, sample, plantManager, defaults, plants, plantExp.values)
            val humanFactories = List(o.players) { PlayerDecisionFactory.humanBaseline() }
            val learnedFactory = learnedBattleSupportFactory(weights)
            val learnedFactories = List(o.players) { if (it == seat) learnedFactory else PlayerDecisionFactory.humanBaseline() }
            control.add(runOne(factory, runner, grove, humanFactories, o, sample, seat, "CONTROL", plantExp, roundExp), seat)
            learned.add(runOne(factory, runner, grove, learnedFactories, o, sample, seat, "LEARNED_BATTLE_SUPPORT", plantExp, roundExp), seat)
        }
        printReport("CONTROL — Human Battle Support", control, o.games)
        println(); printReport("LEARNED — Learned Battle Support", learned, o.games)
        println(); println("DELTA LEARNED - CONTROL")
        println("  win share: ${pp(learned.win/o.games - control.win/o.games)}")
        println("  total VP: ${fmt(learned.totalVp/o.games - control.totalVp/o.games)}")
        println("  Battle VP: ${fmt(learned.battleVp/o.games - control.battleVp/o.games)}")
        println(); printPathologyCheck(learned)
        println("Unusual usage is a diagnostic signal only; confirm with controlled follow-up before calling any behavior an exploit.")
    } finally { app.close() }
}

private fun runOne(factory:GameFactory, runner:GameRunner, grove:List<dugsolutions.leaf.v35.plant.domain.PlantCard>, decisions:List<PlayerDecisionFactory>, o:BattleSupportEvalOptions, sample:Int, seat:Int, variant:String, plantExp:PlantExperimentResearchConfig, roundExp:RoundExperimentResearchConfig): CompletedBattleSupportEvalGame {
    val game = factory(GameConfig(selectedPlantCards=grove, playerDecisionFactories=decisions, roundSetup=o.roundSetup, seed=o.seed+sample, strategySeed=o.strategySeed+sample, plantValues=plantExp.values, roundValues=roundExp.values))
    val result = withSimulationFailureDiagnostics(game, SimulationRunContext("evaluate_battle_support_policy",sample,variant,seat,o.seed+sample,o.strategySeed+sample,GrovePlantCode.encode(grove),o.roundLabel)) { runner.run(game) }
    return CompletedBattleSupportEvalGame(GameSummaryExtractor.extract(game,result), game.chronicle.entries.toList())
}

private class EvalAccumulator {
    var win=0.0; var totalVp=0.0; var battleVp=0.0; var plantVp=0.0; var wounds=0.0
    var sunlightOpp=0.0; var sunlightUses=0.0; var sunlightDraw=0.0; var sunlightPlant=0.0; var sunlightRound=0.0
    var sunImmediate=0.0; var sunDecisive=0.0; var sunWound=0.0
    val supports = linkedMapOf<SupportActionKind,Int>(); var finalMains=0
    val tokenUses = linkedMapOf<SharedTokenResource,Double>(); val tokenZeroGames=linkedMapOf<SharedTokenResource,Int>()
    fun add(g:CompletedBattleSupportEvalGame, seat:Int) {
        val p=g.summary.players.single{it.seat==seat}; win+=p.winShare; totalVp+=p.totalVp; battleVp+=p.battleStrikeVp; plantVp+=p.plantVp; wounds+=p.woundsTaken
        sunlightOpp+=p.sunlightSupportOpportunities; sunlightUses+=p.sunlightSupportUses; sunlightDraw+=p.sunlightExtraDrawActions; sunlightPlant+=p.sunlightExtraPlantActions; sunlightRound+=p.sunlightExtraRoundEffectActions
        sunImmediate+=p.sunlightImmediateStrikeContributions; sunDecisive+=p.sunlightWinnerDecisiveContributions; sunWound+=p.sunlightWoundDecisiveContributions
        g.entries.filterIsInstance<GameEntry.SupportAction>().filter{it.phase==ChroniclePhase.BATTLE && it.playerId==p.playerId}.forEach{supports[it.action]=(supports[it.action]?:0)+1}
        finalMains += g.entries.filterIsInstance<GameEntry.MainAction>().count{it.playerId==p.playerId && it.battleStage==BattleMainStage.FINAL}
        g.summary.sharedTokenEconomy.forEach { e -> tokenUses[e.resource]=(tokenUses[e.resource]?:0.0)+e.spendsOrUses; if(e.reachedZero) tokenZeroGames[e.resource]=(tokenZeroGames[e.resource]?:0)+1 }
    }
}

private fun printReport(label:String,a:EvalAccumulator,n:Int){
    println(label)
    println("  win share: ${pp(a.win/n)}"); println("  total VP: ${fmt(a.totalVp/n)}; Plant VP: ${fmt(a.plantVp/n)}; Battle VP: ${fmt(a.battleVp/n)}; wounds: ${fmt(a.wounds/n)}")
    println("  Support choice frequencies/game:")
    SupportActionKind.entries.forEach{println("    ${it.name}: ${fmt((a.supports[it]?:0).toDouble()/n)}")}; println("    FINAL_MAIN/proceed: ${fmt(a.finalMains.toDouble()/n)}")
    println("  Sunlight: opportunities/game=${fmt(a.sunlightOpp/n)} uses/game=${fmt(a.sunlightUses/n)} rate=${pp(if(a.sunlightOpp==0.0)0.0 else a.sunlightUses/a.sunlightOpp)}")
    println("    funded Draw=${fmt(a.sunlightDraw/n)} Plant=${fmt(a.sunlightPlant/n)} Round=${fmt(a.sunlightRound/n)}")
    println("    immediate Strike contributions=${fmt(a.sunImmediate/n)} winner-decisive=${fmt(a.sunDecisive/n)} Wound-decisive=${fmt(a.sunWound/n)}")
    println("  TOKEN ECONOMY (whole-table physical supply):")
    SharedTokenResource.entries.forEach { r -> println("    ${r.name}: uses/game=${fmt((a.tokenUses[r]?:0.0)/n)} reached-zero games=${a.tokenZeroGames[r]?:0}/${n}") }
}

private fun printPathologyCheck(a:EvalAccumulator){
    println("SUPPORT USAGE EXTREMES — DESIGNER REVIEW")
    val total=a.supports.values.sum()+a.finalMains
    if(total==0){println("  no recorded Step-5 choices"); return}
    a.supports.toList().sortedByDescending{it.second}.forEach{(k,v)-> val share=v.toDouble()/total; if(share>=.90 || v==0) println("  ${k.name}: ${pp(share)} of recorded Step-5 choices") }
    SupportActionKind.entries.filter{(a.supports[it]?:0)==0}.forEach{println("  ${it.name}: never selected in held-out affected-player Battles")}
}
private fun fmt(v:Double)="%.2f".format(v); private fun pp(v:Double)="%.2f%%".format(v*100)

internal data class BattleSupportEvalOptions(val games:Int,val seed:Long,val strategySeed:Long,val weights:Path,val plantOverrides:Path?,val roundOverrides:Path?,val grovePattern:String?,val groveSeed:Long,val players:Int,val roundLabel:String){
    val roundSetup=parseRoundSetup(roundLabel)
    companion object{fun parse(args:List<String>):BattleSupportEvalOptions{var games=300;var seed=93000L;var strategy=103000L;var weights=Paths.get("output/ai/battle-support-policy-v1-trained.weights");var plant:Path?=null;var round:Path?=null;var grove:String?=null;var groveSeed=113000L;var players=4;var rounds="3/2/2";var i=0;fun value(a:String)=if('=' in a)a.substringAfter('=')else args[++i];
        while(i<args.size){val a=args[i];when{a.startsWith("--games")->games=value(a).toInt();a.startsWith("--seed")->seed=value(a).toLong();a.startsWith("--strategy-seed")->strategy=value(a).toLong();a.startsWith("--weights")->weights=Paths.get(value(a));a.startsWith("--plant-overrides")->plant=Paths.get(value(a));a.startsWith("--round-overrides")->round=Paths.get(value(a));a.startsWith("--grove-pattern")->grove=value(a);a.startsWith("--grove-seed")->groveSeed=value(a).toLong();a.startsWith("--players")->players=value(a).toInt();a.startsWith("--rounds")->rounds=value(a);else->error("Unknown option: $a")};i++};return BattleSupportEvalOptions(games,seed,strategy,weights,plant,round,grove,groveSeed,players,rounds)}}
}
