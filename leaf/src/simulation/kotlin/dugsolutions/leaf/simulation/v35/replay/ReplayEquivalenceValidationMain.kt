package dugsolutions.leaf.simulation.v35.replay

import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.replay.CultivationReplayFork
import dugsolutions.leaf.v35.game.replay.ReplayMainAction
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Paths

/** C5: real-game exact replay and matched-event counterfactual smoke validation. */
fun main(args: Array<String>) {
    val options = args.map { it.split("=", limit=2) }.filter { it.size==2 }
        .associate { it[0].removePrefix("--") to it[1] }
    val games = (options["games"] ?: "10").toInt().also { require(it in 1..10000) }
    val seed = (options["seed"] ?: "930000").toLong()
    val out = Paths.get(options["output"] ?: "output/experiments/replay-equivalence-c5")
    Files.createDirectories(out)
    val app = koinApplication { modules(appModules) }
    try {
        val k = app.koin
        loadCultivationResearchCards(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(),
            k.get<WispCardRegistry>(), k.get<WispCardManager>(), k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
        val plants = k.get<PlantCardManager>()
        val grove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plants.getCard(it)) }
        val engine = CompostCounterfactualRunner(k.get<GameFactory>(), k.get<GameRunner>(), grove,
            List(2) { PlayerDecisionFactory.humanBaseline() }, parseRoundSetup("3/2/2"),
            eventBasedDieRolls=true, sharedPercentileDieRolls=true)
        val report = out.resolve("pairs.tsv")
        Files.writeString(report, "seed\tmode\texact\tmatched_same_size\tmatched_cross_size\tmismatches\tcontrol_only\tcomparison_only\tchanged_decisions\tfirst_changed\n")
        var compared = 0
        repeat(games) { n ->
            val ms = seed+n
            val control = engine.run(ms, ms+100009, retainChronicle=true)
            val repeat = engine.run(ms, ms+100009, retainChronicle=true)
            val exact = ReplayEquivalenceDiagnostics.compare(control, repeat)
            check(exact.exact) { "Exact replay failed at seed=$ms: $exact" }
            check(exact.rolls.unexpectedMismatches==0 && exact.rolls.controlOnly==0 && exact.rolls.comparisonOnly==0) {
                "Exact roll ledger failed at seed=$ms: ${exact.rolls}"
            }
            writeRow(report,ms,"exact",exact)
            val candidate = control.decisions.firstOrNull { d ->
                ReplayMainAction.DRAW in d.legal && d.actualChoice != ReplayMainAction.DRAW
            }
            if (candidate != null) {
                val player = control.summary.players.single { it.playerId.value==candidate.playerId }.playerId
                val changed = engine.run(ms, ms+100009,
                    CultivationReplayFork(candidate.roundNumber,player,candidate.consultationIndex,ReplayMainAction.DRAW),true)
                val diff = ReplayEquivalenceDiagnostics.compare(control,changed)
                check(diff.rolls.unexpectedMismatches == 0) {
                    "Matched roll input changed at seed=$ms: ${diff.rolls}"
                }
                writeRow(report,ms,"fork",diff)
                compared++
            }
        }
        Files.writeString(out.resolve("summary.txt"),
            "Exact reproducibility: $games/$games seeds passed.\n"+
            "Forked games checked: $compared.\n"+
            "All matched keyed rolls preserved their percentile.\n"+
            "Coverage limitation: non-keyed RNG events are not in this ledger.\n")
        println("C5 PASSED: $games exact replays; $compared forked counterfactuals; reports=$out")
    } finally { app.close() }
}

private fun writeRow(path: java.nio.file.Path, seed: Long, mode: String, report: ReplayEquivalenceReport) {
    val r = report.rolls
    Files.writeString(path, listOf(seed,mode,report.exact,r.matchedSameSize,r.matchedCrossSize,
        r.unexpectedMismatches,r.controlOnly,r.comparisonOnly,report.decisions.changed,
        report.decisions.firstChanged.orEmpty()).joinToString("\t")+"\n",java.nio.file.StandardOpenOption.APPEND)
}
