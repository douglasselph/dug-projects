package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.*
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.learned.buy.*
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

/** M3-G1: matched held-out evaluation of one trained Buy policy against Human Baseline control. */
fun main(args: Array<String>) {
    val o = EvalOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val allPlants = plantManager.getAllCards().cards
        val grove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val raw = LearnedBuyWeights.load(o.input)
        require(raw.provenance.trainingStatus == "trained") {
            "Held-out evaluation requires a trained policy; ${o.input} has trainingStatus=${raw.provenance.trainingStatus}"
        }
        val weights = LearnedBuyCardCatalog.prepare(raw, allPlants)
        rejectTrainingSeedOverlap(weights, o)
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val plantsByName = allPlants.associateBy { it.name }
        val control = EvalAccumulator()
        val learned = EvalAccumulator()

        println("M3-G1 — Learned Buy Policy Held-Out Evaluation")
        println("policy=${o.input}")
        println("matched samples=${o.games}; games run=${o.games * 2}")
        println("evaluation seeds=${o.seed}..${o.seed + o.games - 1}; strategy seeds=${o.strategySeed}..${o.strategySeed + o.games - 1}")
        println("affected role rotates across physical seats; opponents=Human Baseline; Grove=FirstGameDefault; rounds=3/2/2")
        println("CONTROL=Human Baseline; LEARNED=same role with learned Buy selection only; all other decisions=Human Baseline")
        println("training fitness recorded in policy=${weights.provenance.fitness?.let(::pct) ?: "unknown"}; evaluation seeds are required not to overlap its recorded training cohorts")
        println()

        repeat(o.games) { sample ->
            val seat = sample % 4
            val mechanicalSeed = o.seed + sample
            val strategySeed = o.strategySeed + sample
            val controlFactories = List(4) { PlayerDecisionFactory.humanBaseline() }
            val learnedFactories = List(4) { if (it == seat) learnedFactory(weights) else PlayerDecisionFactory.humanBaseline() }
            control.add(runOne(factory, runner, grove, controlFactories, mechanicalSeed, strategySeed, sample, seat, "CONTROL"), seat, plantsByName)
            learned.add(runOne(factory, runner, grove, learnedFactories, mechanicalSeed, strategySeed, sample, seat, "LEARNED"), seat, plantsByName)
        }
        printReport(o, weights, control, learned)
    } finally { app.close() }
}

internal data class CompletedEvalGame(val summary: GameSummary, val entries: List<GameEntry>)

private fun runOne(factory: GameFactory, runner: GameRunner, grove: List<PlantCard>, decisions: List<PlayerDecisionFactory>, seed: Long, strategySeed: Long, sample: Int, affectedSeat: Int, variant: String): CompletedEvalGame {
    val game = factory(GameConfig(selectedPlantCards=grove, playerDecisionFactories=decisions, roundSetup=GameRoundSetup.standard(), seed=seed, strategySeed=strategySeed, recordDecisionReasoning=false))
    val result = withSimulationFailureDiagnostics(game, SimulationRunContext("evaluate_buy_policy", sample, variant, affectedSeat, seed, strategySeed, "FirstGameDefault", "3/2/2")) { runner.run(game) }
    return CompletedEvalGame(GameSummaryExtractor.extract(game, result), game.chronicle.entries.toList())
}

internal class EvalAccumulator {
    var winShare=0.0; var vp=0L; var plants=0L; var plantCost=0L; var dice=0L; var dicePower=0L; var battleVp=0L; var wounds=0L
    var plantPurchases=0L; var diePurchases=0L
    val seatWins=DoubleArray(4); val seatGames=IntArray(4)
    val plantCosts=sortedMapOf<Int,Long>(); val plantTypes=sortedMapOf<String,Long>(); val plantCards=sortedMapOf<String,Long>(); val dieSizes=sortedMapOf<String,Long>()

    fun add(game: CompletedEvalGame, seat: Int, plantsByName: Map<String,PlantCard>) {
        val p=game.summary.players.single { it.seat==seat }
        winShare+=p.winShare; vp+=p.totalVp; plants+=p.finalPlantCount; plantCost+=p.finalPlantPrintedCost; dice+=p.finalDiceCount; dicePower+=p.finalDicePower; battleVp+=p.battleStrikeVp; wounds+=p.woundsTaken
        seatWins[seat]+=p.winShare; seatGames[seat]++
        game.entries.filterIsInstance<GameEntry.Purchase>().filter { it.playerId==p.playerId }.forEach { purchase ->
            when(purchase.kind) {
                PurchaseKind.PLANT -> { plantPurchases++; plantCosts.bump(purchase.cost); plantCards.bump(purchase.itemName); plantTypes.bump(plantsByName[purchase.itemName]?.type?.name ?: "UNKNOWN") }
                PurchaseKind.DIE -> { diePurchases++; dieSizes.bump(purchase.itemName) }
            }
        }
    }
    private fun <K> MutableMap<K,Long>.bump(key:K) { this[key]=(this[key]?:0L)+1L }
}

private fun printReport(o:EvalOptions, weights:LearnedBuyWeights, c:EvalAccumulator, l:EvalAccumulator) {
    fun avg(x:Long)="%.2f".format(x.toDouble()/o.games); fun delta(a:Double,b:Double)="%+.2f".format(b-a)
    val cw=c.winShare/o.games; val lw=l.winShare/o.games
    println("Held-out result")
    println("  Control win share: ${pct(cw)}")
    println("  Learned win share: ${pct(lw)}")
    println("  Win-share delta:   ${signedPct(lw-cw)}")
    println("  Avg VP:             control=${avg(c.vp)} learned=${avg(l.vp)} delta=${delta(c.vp.toDouble()/o.games,l.vp.toDouble()/o.games)}")
    println("  Avg final Plants:   control=${avg(c.plants)} learned=${avg(l.plants)}")
    println("  Avg Plant cost:     control=${avg(c.plantCost)} learned=${avg(l.plantCost)}")
    println("  Avg final dice:     control=${avg(c.dice)} learned=${avg(l.dice)}")
    println("  Avg die-side power: control=${avg(c.dicePower)} learned=${avg(l.dicePower)}")
    println("  Avg Battle VP:      control=${avg(c.battleVp)} learned=${avg(l.battleVp)}")
    println("  Avg Wounds:         control=${avg(c.wounds)} learned=${avg(l.wounds)}")
    println()
    println("Affected-role win share by physical seat")
    for(s in 0..3) { val n=c.seatGames[s]; println("  Seat ${s+1} (n=$n): control=${pct(c.seatWins[s]/n)} learned=${pct(l.seatWins[s]/n)} delta=${signedPct(l.seatWins[s]/n-c.seatWins[s]/n)}") }
    println()
    println("Buy behavior (affected role; totals across ${o.games} games)")
    println("  Plant purchases: control=${c.plantPurchases} learned=${l.plantPurchases}; avg/game=${avg(c.plantPurchases)} -> ${avg(l.plantPurchases)}")
    println("  Die purchases:   control=${c.diePurchases} learned=${l.diePurchases}; avg/game=${avg(c.diePurchases)} -> ${avg(l.diePurchases)}")
    printCounts("Plant purchases by printed cost",c.plantCosts,l.plantCosts)
    printCounts("Plant purchases by type",c.plantTypes,l.plantTypes)
    printCounts("Die purchases by size",c.dieSizes,l.dieSizes)
    printCounts("Individual Plant acquisitions",c.plantCards,l.plantCards)
    println()
    println("Policy provenance")
    println("  trained rounds=${weights.provenance.roundPattern}; Grove=${weights.provenance.grove}; generations=${weights.provenance.generations}; games/policy=${weights.provenance.gamesPerPolicy}; training fitness=${weights.provenance.fitness?.let(::pct) ?: "unknown"}")
    println("  training mechanical seed start=${weights.provenance.mechanicalSeedStart}; strategy seed start=${weights.provenance.strategySeedStart}")
    println("  evaluation mechanical seeds=${o.seed}..${o.seed+o.games-1}; strategy seeds=${o.strategySeed}..${o.strategySeed+o.games-1}")
    println()
    println("Interpretation: this is held-out evidence for this policy on FirstGameDefault with 3/2/2, not evidence for other Groves or round structures.")
    println("Note: Done-by-stage is not reported yet because the production Chronicle records purchases, not a no-purchase Buy decision; M3-G1 does not alter production decision tracing merely to manufacture that metric.")
}

private fun <K:Comparable<K>> printCounts(title:String,c:Map<K,Long>,l:Map<K,Long>) { println("  $title:"); (c.keys+l.keys).toSortedSet().forEach { k -> println("    $k: control=${c[k]?:0} learned=${l[k]?:0}") } }
private fun pct(x:Double)="%.2f%%".format(x*100.0)
private fun signedPct(x:Double)=(if(x>=0) "+" else "")+pct(x)

private fun rejectTrainingSeedOverlap(weights:LearnedBuyWeights,o:EvalOptions) {
    val n=weights.provenance.gamesPerPolicy ?: return
    fun overlaps(a0:Long,a1:Long,b0:Long,b1:Long)=a0<=b1 && b0<=a1
    weights.provenance.mechanicalSeedStart?.let { start -> require(!overlaps(o.seed,o.seed+o.games-1,start,start+n-1)) { "Evaluation mechanical seeds overlap recorded training seeds $start..${start+n-1}; choose held-out --seed values." } }
    weights.provenance.strategySeedStart?.let { start -> require(!overlaps(o.strategySeed,o.strategySeed+o.games-1,start,start+n-1)) { "Evaluation strategy seeds overlap recorded training seeds $start..${start+n-1}; choose held-out --strategy-seed values." } }
}

private fun learnedFactory(weights:LearnedBuyWeights):PlayerDecisionFactory=object:PlayerDecisionFactory { override fun create()=LearnedBuy.createDirector(weights); override fun create(strategyRandomizer:StrategyRandomizer)=LearnedBuy.createDirector(weights,strategyRandomizer); override fun create(strategyRandomizer:StrategyRandomizer,reasoningSink:DecisionReasoningSink)=LearnedBuy.createDirector(weights,strategyRandomizer) }
private fun loadCards(plantRegistry:PlantCardRegistry,plantManager:PlantCardManager,wispRegistry:WispCardRegistry,wispManager:WispCardManager,roundRegistry:RoundCardRegistry,roundManager:RoundCardManager){ val root=CardDataFiles.dataDirectory(); plantRegistry.clear(); plantRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST,root),CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST,root)); plantManager.loadCards(plantRegistry); wispRegistry.clear(); wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST,root)); wispManager.loadCards(wispRegistry); roundRegistry.clear(); roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST,root)); roundManager.loadCards(roundRegistry) }

private data class EvalOptions(val games:Int,val seed:Long,val strategySeed:Long,val input:Path){ companion object { fun parse(args:List<String>):EvalOptions { var games=1000; var seed=161000L; var strategy=171000L; var input=Paths.get("output/ai/buy-policy-v1-trained.weights"); var positional=false; var i=0; fun value(a:String)=if('=' in a)a.substringAfter('=')else args[++i]; while(i<args.size){val a=args[i];when{ !positional&&a.matches(Regex("[1-9][0-9]*"))->{games=a.toInt();positional=true}; a.startsWith("--games")->games=value(a).toInt(); a.startsWith("--seed")->seed=value(a).toLong(); a.startsWith("--strategy-seed")->strategy=value(a).toLong(); a.startsWith("--input")->input=Paths.get(value(a)); a=="--help"->{usage();kotlin.system.exitProcess(0)}; else->error("Unknown argument: $a")};i++ }; require(games>0); return EvalOptions(games,seed,strategy,input) }; private fun usage()=println("evaluate_buy_policy [N|--games N] [--seed N] [--strategy-seed N] [--input PATH]") } }
