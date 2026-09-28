package dugsolutions.leaf.simulation.v35.experiment

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.strategy.lean.LeanCreatureStrategy
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.*
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication

fun main(args: Array<String>) {
    val options = Options.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        val root = CardDataFiles.dataDirectory()
        val plantRegistry = koin.get<PlantCardRegistry>().also {
            it.clear(); it.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST, root), CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST, root))
        }
        koin.get<PlantCardManager>().loadCards(plantRegistry)
        val wispRegistry = koin.get<WispCardRegistry>().also {
            it.clear(); it.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST, root))
        }
        koin.get<WispCardManager>().loadCards(wispRegistry)
        val roundRegistry = koin.get<RoundCardRegistry>().also {
            it.clear(); it.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST, root))
        }
        koin.get<RoundCardManager>().loadCards(roundRegistry)

        val plantManager = koin.get<PlantCardManager>()
        val plants = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val control = Accumulator()
        val lean = Accumulator()

        repeat(options.games) { sample ->
            val mechanicalSeed = options.seed + sample
            val strategySeed = options.strategySeed + sample
            val affectedSeat = sample % 4

            val controlFactories = List(4) { PlayerDecisionFactory.humanBaseline() }
            val leanFactories = List(4) { seat ->
                if (seat == affectedSeat) LeanCreatureStrategy.decisionFactory() else PlayerDecisionFactory.humanBaseline()
            }
            control.add(runOne(factory, runner, plants, controlFactories, mechanicalSeed, strategySeed), affectedSeat)
            lean.add(runOne(factory, runner, plants, leanFactories, mechanicalSeed, strategySeed), affectedSeat)
        }
        printReport(options, control, lean)
    } finally { app.close() }
}

private fun runOne(
    factory: GameFactory,
    runner: GameRunner,
    plants: List<dugsolutions.leaf.v35.plant.domain.PlantCard>,
    decisions: List<PlayerDecisionFactory>,
    seed: Long,
    strategySeed: Long
): GameSummary {
    val game = factory(GameConfig(
        selectedPlantCards = plants,
        playerDecisionFactories = decisions,
        roundSetup = GameRoundSetup.standard(),
        seed = seed,
        strategySeed = strategySeed,
        recordDecisionReasoning = false
    ))
    return GameSummaryExtractor.extract(game, runner.run(game))
}

private class Accumulator {
    var winShare = 0.0; var vp = 0L; var plants = 0L; var plantCost = 0L
    var dice = 0L; var dicePower = 0L; var battleVp = 0L; var wounds = 0L
    var d4=0L; var d6=0L; var d8=0L; var d10=0L; var d12=0L; var d20=0L
    var exactShape = 0; var atLeastThreeTargets = 0; var targetCards = 0L
    val seatWins = DoubleArray(4); val seatGames = IntArray(4)

    fun add(summary: GameSummary, seat: Int) {
        val p = summary.players.single { it.seat == seat }
        winShare += p.winShare; vp += p.totalVp; plants += p.finalPlantCount; plantCost += p.finalPlantPrintedCost
        dice += p.finalDiceCount; dicePower += p.finalDicePower; battleVp += p.battleStrikeVp; wounds += p.woundsTaken
        with(p.ownedDiceSignature) {
            this@Accumulator.d4 += d4
            this@Accumulator.d6 += d6
            this@Accumulator.d8 += d8
            this@Accumulator.d10 += d10
            this@Accumulator.d12 += d12
            this@Accumulator.d20 += d20
        }
        val names = p.plantCreatureSignature.cards.map { it.plantName }
        val vines = names.count { it == LeanCreatureStrategy.VINE_9 || it == LeanCreatureStrategy.VINE_11 }
        val flowers = names.count { it == LeanCreatureStrategy.FLOWER_17 }
        val targets = vines + flowers
        targetCards += targets
        if (targets >= 3) atLeastThreeTargets++
        if (vines == 2 && flowers == 2 && p.finalPlantCount == 4) exactShape++
        seatWins[seat] += p.winShare; seatGames[seat]++
    }
}

private fun printReport(o: Options, c: Accumulator, l: Accumulator) {
    fun pct(x: Double) = "%.2f%%".format(x * 100)
    fun avg(x: Long) = "%.2f".format(x.toDouble()/o.games)
    fun delta(a: Double, b: Double) = "%+.2f".format(b-a)
    println("Focused Plant Shape — Lean Creature Experiment")
    println("Matched games per condition: ${o.games}")
    println("Grove: FirstGameDefault")
    println("Round structure: 3/2/2")
    println("Lean Plant goal: exactly 2 Vines from {Vine_09_01, Vine_11_04} + 2 Flower_17_04; no other Plants")
    println("Lean dice policy: below 7 dice buy D10+; at 7+ dice buy only D12/D20; prefer legal Compost while Hand contains a die below D12")
    println("All other decisions: Human Baseline")
    println("Mechanical seeds: ${o.seed}..${o.seed+o.games-1}; strategy seeds: ${o.strategySeed}..${o.strategySeed+o.games-1}")
    println("Experimental role rotates through all four physical seats.")
    println()
    val cw=c.winShare/o.games; val lw=l.winShare/o.games
    println("Affected role")
    println("  Control win share: ${pct(cw)}")
    println("  Lean win share:    ${pct(lw)}")
    println("  Win-share delta:   ${if(lw-cw>=0) "+" else ""}${pct(lw-cw)}")
    println("  Avg VP:            control=${avg(c.vp)} lean=${avg(l.vp)} delta=${delta(c.vp.toDouble()/o.games,l.vp.toDouble()/o.games)}")
    println("  Avg Plant count:   control=${avg(c.plants)} lean=${avg(l.plants)}")
    println("  Avg printed cost:  control=${avg(c.plantCost)} lean=${avg(l.plantCost)}")
    println("  Avg dice count:    control=${avg(c.dice)} lean=${avg(l.dice)}")
    println("  Avg die-side power: control=${avg(c.dicePower)} lean=${avg(l.dicePower)}")
    println("  Avg Battle VP:     control=${avg(c.battleVp)} lean=${avg(l.battleVp)}")
    println("  Avg Wounds:        control=${avg(c.wounds)} lean=${avg(l.wounds)}")
    println()
    println("Lean feasibility")
    println("  Exact 4-card target shape: ${l.exactShape}/${o.games} (${pct(l.exactShape.toDouble()/o.games)})")
    println("  At least 3 target cards:   ${l.atLeastThreeTargets}/${o.games} (${pct(l.atLeastThreeTargets.toDouble()/o.games)})")
    println("  Avg target cards owned:    ${avg(l.targetCards)}")
    println("  Avg final dice profile:    D4=${avg(l.d4)} D6=${avg(l.d6)} D8=${avg(l.d8)} D10=${avg(l.d10)} D12=${avg(l.d12)} D20=${avg(l.d20)}")
    println("  Avg D10+ dice:             ${avg(l.d10+l.d12+l.d20)}")
    println()
    println("Affected-role win share by physical seat")
    for (s in 0..3) {
        val n=c.seatGames[s]
        if (n == 0) println("  Seat ${s+1} (n=0): not represented")
        else println("  Seat ${s+1} (n=$n): control=${pct(c.seatWins[s]/n)} lean=${pct(l.seatWins[s]/n)}")
    }
    println()
    println("Note: this first feasibility probe uses FirstGameDefault only; Grove sensitivity should be a follow-up if the lean shape proves viable.")
}

private data class Options(val games:Int,val seed:Long,val strategySeed:Long) {
    companion object { fun parse(args:List<String>):Options {
        var games=1000; var seed=31000L; var strategy=41000L; var positional=false
        var i=0
        while(i<args.size){ val a=args[i]; when {
            !positional && a.matches(Regex("[1-9][0-9]*")) -> { games=a.toInt(); positional=true; i++ }
            a=="--seed" -> { seed=args[++i].toLong(); i++ }
            a.startsWith("--seed=") -> { seed=a.substringAfter('=').toLong(); i++ }
            a=="--strategy-seed" -> { strategy=args[++i].toLong(); i++ }
            a.startsWith("--strategy-seed=") -> { strategy=a.substringAfter('=').toLong(); i++ }
            else -> error("Unknown argument: $a")
        }}
        require(games>0); return Options(games,seed,strategy)
    }}
}
