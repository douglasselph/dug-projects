package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.v35.chronicle.ChronicleTextRenderer
import dugsolutions.leaf.v35.chronicle.domain.*
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.*
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Global credibility check for Human Baseline players. No behavior is changed here. */
fun main(args: Array<String>) {
    val o = GlobalCertificationOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin=app.koin
        loadGlobalCertificationCards(koin.get(),koin.get(),koin.get(),koin.get(),koin.get(),koin.get())
        val pm=koin.get<PlantCardManager>(); val allPlants=pm.getAllCards().cards
        val defaults=FirstGameDefault.PLANT_NAMES.map { requireNotNull(pm.getCard(it)) }
        val factory=koin.get<GameFactory>(); val runner=koin.get<GameRunner>()
        Files.createDirectories(o.outputDir)
        runCohort("first-game-default", o.games, o.players, o.seed, o.strategySeed, null, o.groveSeed, defaults, pm, allPlants.associateBy{it.name}, factory, runner, o.outputDir)
        runCohort("random-grove", o.games, o.players, o.seed+o.games, o.strategySeed+o.games, "000000000", o.groveSeed, defaults, pm, allPlants.associateBy{it.name}, factory, runner, o.outputDir)
    } finally { app.close() }
}

private fun runCohort(label:String,games:Int,players:Int,seed:Long,strategySeed:Long,pattern:String?,groveSeed:Long,defaults:List<dugsolutions.leaf.v35.plant.domain.PlantCard>,pm:PlantCardManager,plantsByName:Map<String,dugsolutions.leaf.v35.plant.domain.PlantCard>,factory:GameFactory,runner:GameRunner,out:Path) {
    val a=EvalAccumulator(players, marketCards = plantsByName.values.toList()); var refreshes=0L; var plantActivations=0L
    val reps=mutableListOf<Representative>()
    repeat(games) { i ->
        val grove=if(pattern==null) defaults else GrovePlantCode.overrideNames(GrovePlantCode.generate(pattern,Randomizer.create(groveSeed+i))).map { requireNotNull(pm.getCard(it)) }
        val ms=seed+i; val ss=strategySeed+i
        val game=factory(GameConfig(selectedPlantCards=grove,playerDecisionFactories=List(players){PlayerDecisionFactory.humanBaseline()},roundSetup=GameRoundSetup.standard(),seed=ms,strategySeed=ss,recordDecisionReasoning=false))
        val result=withSimulationFailureDiagnostics(game,SimulationRunContext("human_baseline_global_certification",i,label,null,ms,ss,GrovePlantCode.encode(grove),"3/2/2")){runner.run(game)}
        val completed=CompletedEvalGame(GameSummaryExtractor.extract(game,result),game.chronicle.entries.toList())
        for(seat in 0 until players) a.add(completed,seat,plantsByName,grove)
        refreshes += game.chronicle.entries.filterIsInstance<GameEntry.Refresh>().size
        plantActivations += game.chronicle.entries.filterIsInstance<GameEntry.EffectResolved>().count { it.sourceKind==EffectSourceKind.PLANT }
        val totalVp=completed.summary.players.sumOf{it.totalVp}; val wounds=completed.summary.players.sumOf{it.woundsTaken}; val plants=completed.summary.players.sumOf{it.finalPlantCount}
        reps += Representative(ms,ss,GrovePlantCode.encode(grove),totalVp,wounds,plants,ChronicleTextRenderer.render(game.chronicle.entries,selectedPlantCards=grove))
    }
    val report=renderGlobal(label,games,players,seed,strategySeed,pattern,groveSeed,a,refreshes,plantActivations)
    val reportPath=out.resolve("$label-report.txt"); Files.writeString(reportPath,report); print(report)
    val selected=(reps.sortedBy{it.totalVp}.take(1)+reps.sortedByDescending{it.totalVp}.take(1)+reps.sortedByDescending{it.wounds}.take(1)+reps.sortedBy{it.plants}.take(1)).distinctBy{it.mechanicalSeed}
    selected.forEachIndexed { n,r -> Files.writeString(out.resolve("$label-chronicle-${n+1}-seed-${r.mechanicalSeed}.txt"), "mechanicalSeed=${r.mechanicalSeed} strategySeed=${r.strategySeed} grove=${r.grove}\n${r.text}") }
    println("Representative Chronicles: ${selected.joinToString { "seed=${it.mechanicalSeed}" }} written under $out")
}

private data class Representative(val mechanicalSeed:Long,val strategySeed:Long,val grove:String,val totalVp:Int,val wounds:Int,val plants:Int,val text:String)

private fun renderGlobal(label:String,games:Int,players:Int,seed:Long,strategySeed:Long,pattern:String?,groveSeed:Long,a:EvalAccumulator,refreshes:Long,plantActivations:Long):String=buildString {
    val playerGames=games*players.toLong()
    fun avg(v:Long)="%.2f".format(v.toDouble()/playerGames)
    fun pct(v:Double)="%.2f%%".format(v*100)
    fun map(title:String,m:Map<*,Long>,den:Long=playerGames){appendLine("  $title:"); m.entries.sortedByDescending{it.value}.forEach{appendLine("    ${it.key}: ${it.value} (${"%.2f".format(it.value.toDouble()/den)} per player-game)")}}
    appendLine("Human Baseline Global Certification — $label")
    appendLine("games=$games players=$players player-games=$playerGames rounds=3/2/2 mechanicalSeeds=$seed..${seed+games-1} strategySeeds=$strategySeed..${strategySeed+games-1}")
    appendLine(if(pattern==null) "Grove=FirstGameDefault" else "Grove=random per game pattern=$pattern groveSeeds=$groveSeed..${groveSeed+games-1}")
    appendLine("Seat win shares:"); for(s in 0 until players) appendLine("  Seat ${s+1}: ${pct(a.seatWins[s]/a.seatGames[s])} (n=${a.seatGames[s]})")
    appendLine("Core outcomes per player-game: VP=${avg(a.vp)} BattleVP=${avg(a.battleVp)} Wounds=${avg(a.wounds)}")
    appendLine("Final development per player-game: Plants=${avg(a.plants)} printedCost=${avg(a.plantCost)} dice=${avg(a.dice)} diePower=${avg(a.dicePower)} avgSides=${"%.2f".format(a.dicePower.toDouble()/a.dice)}")
    map("Final dice by size",a.finalDiceSizes)
    appendLine("Buy behavior: Plants=${avg(a.plantPurchases)} dice=${avg(a.diePurchases)} purchases/player-game=${"%.2f".format((a.plantPurchases+a.diePurchases).toDouble()/playerGames)}")
    map("Plant purchases by cost",a.plantCosts); map("Plant purchases by type",a.plantTypes); map("Die purchases by size",a.dieSizes); map("Individual Plant acquisitions",a.plantCards)
    appendLine("Buy phases=${a.buyShape.phases}; purchases/phase=${"%.2f".format(a.buyShape.purchases.toDouble()/a.buyShape.phases)} startingPower=${"%.2f".format(a.buyShape.startingPower.toDouble()/a.buyShape.phases)} spent=${"%.2f".format(a.buyShape.spentPower.toDouble()/a.buyShape.phases)} overpayment=${"%.2f".format(a.buyShape.overpayment.toDouble()/a.buyShape.phases)}")
    appendLine("Plant activation/effects=${"%.2f".format(plantActivations.toDouble()/playerGames)} per player-game; Creature Refresh=${"%.2f".format(refreshes.toDouble()/playerGames)} per player-game")
    map("Plant effects resolved",a.utilization.plantEffects); map("Battle support actions",a.utilization.supportActions); map("Die upgrades",a.utilization.upgrades); map("Upgrade sources",a.utilization.upgradeSources)
    appendLine("Wisps: roll-gained=${"%.2f".format(a.utilization.rollWispsGained.toDouble()/playerGames)} immediate-play=${"%.2f".format(a.utilization.immediateWispsPlayed.toDouble()/playerGames)} final=${"%.2f".format(a.utilization.finalWisps.toDouble()/playerGames)} finalVP=${"%.2f".format(a.utilization.finalWispVp.toDouble()/playerGames)}")
    map("Wisp effects resolved",a.utilization.wispEffects)
    appendLine("Battle entry/use by Battle:"); a.battleShape.battleCount.forEach{(b,n)-> appendLine("  Battle $b: VP=${"%.2f".format((a.battleShape.vp[b]?:0).toDouble()/n)} wounds=${"%.2f".format((a.battleShape.wounds[b]?:0).toDouble()/n)} poolDice=${"%.2f".format((a.battleShape.poolDice[b]?:0).toDouble()/n)} usedDice=${"%.2f".format((a.battleShape.usedDice[b]?:0).toDouble()/n)}") }
    appendLine("Top-level anomaly aids: inspect extreme representative Chronicles emitted beside this report; individual Plant acquisition/effect frequencies above are intended to expose cards that are never used or dominate ordinary play.")
    appendLine()
}

private data class GlobalCertificationOptions(val games:Int,val players:Int,val seed:Long,val strategySeed:Long,val groveSeed:Long,val outputDir:Path){companion object{fun parse(args:List<String>):GlobalCertificationOptions{fun value(name:String,default:String)=args.firstOrNull{it.startsWith("--$name=")}?.substringAfter('=')?:default;
    val players=value("players","4").toInt(); require(players in 2..4) { "--players must be 2, 3, or 4" }
    val defaultOutput = if (players == 4) "output/human-baseline-global-certification" else "output/human-baseline-global-certification/${players}-player"
    return GlobalCertificationOptions(value("games","1000").toInt(),players,value("seed","361000").toLong(),value("strategy-seed","371000").toLong(),value("grove-seed","381000").toLong(),Paths.get(value("output",defaultOutput)))}}}

private fun loadGlobalCertificationCards(plantRegistry:PlantCardRegistry,plantManager:PlantCardManager,wispRegistry:WispCardRegistry,wispManager:WispCardManager,roundRegistry:RoundCardRegistry,roundManager:RoundCardManager){
    val root=CardDataFiles.dataDirectory()
    plantRegistry.clear(); plantRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST,root),CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST,root)); plantManager.loadCards(plantRegistry)
    wispRegistry.clear(); wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST,root)); wispManager.loadCards(wispRegistry)
    roundRegistry.clear(); roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST,root)); roundManager.loadCards(roundRegistry)
}
