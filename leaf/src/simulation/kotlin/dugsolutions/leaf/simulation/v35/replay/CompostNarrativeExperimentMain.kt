package dugsolutions.leaf.simulation.v35.replay

import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
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
import java.nio.file.Path
import java.nio.file.Paths

/** A real-game evidence generator. Within-arm pairs change ONE Build action; all policies then adapt. */
fun main(args: Array<String>) {
    val opts = args.map { it.split("=", limit = 2) }.filter { it.size == 2 }.associate { it[0].removePrefix("--") to it[1] }
    fun arg(name: String, default: String) = opts[name] ?: default
    val games = arg("games", "200").toInt().also { require(it in 1..100000) }
    val keep = arg("chronicles", "12").toInt().also { require(it >= 0) }
    val seed = arg("seed", "720000").toLong()
    val out = Paths.get(arg("output", "output/experiments/compost-narrative"))
    val roundsCsv = Paths.get(arg("round-overrides", "output/experiments/compost-narrative/config/original.csv"))
    val arm = arg("arm", "original")
    Files.createDirectories(out)
    val app = koinApplication { modules(appModules) }
    try {
        val k = app.koin
        loadCultivationResearchCards(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(), k.get<WispCardRegistry>(),
            k.get<WispCardManager>(), k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
        val plants = k.get<PlantCardManager>()
        val grove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plants.getCard(it)) }
        val roundValues = RoundExperimentResearchConfig.resolve(roundsCsv, k.get<RoundCardManager>().getAllCards().cards).values
        val engine = CompostCounterfactualRunner(k.get<GameFactory>(), k.get<GameRunner>(), grove,
            List(4) { PlayerDecisionFactory.humanBaseline() }, parseRoundSetup("3/2/2"), roundValues)
        val header = "arm\tseed\tplayer\tround\tconsultation\treplacement\toriginal_vp\talternative_vp\tdelta_vp\tdelta_battle_vp\tdelta_plant_vp\tdelta_wisp_vp\tdelta_wounds\tdelta_dice_count\tdelta_dice_power\tdelta_d4\tdelta_d6\tdelta_d8\tdelta_d10\tdelta_d12\tdelta_d20\tdelta_plants\tdelta_win_share\tcase_dir"
        val tsv = out.resolve("pairs.tsv")
        Files.writeString(tsv, header + "\n")
        var eligible = 0
        var successful = 0
        var saved = 0
        val choices = listOf(ReplayMainAction.DRAW, ReplayMainAction.ROUND_EFFECT_1, ReplayMainAction.ROUND_EFFECT_2)
        repeat(games) { sample ->
            val ms = seed + sample
            // One baseline per seed: all original policy choices are authentic, not artificially forced Compost.
            val base = engine.run(ms, ms + 99173, retainChronicle = false)
            val targets = engine.compostOpportunities(base)
            if (targets.isEmpty()) return@repeat
            eligible++
            // First naturally selected Compost in the entire game is the intervention target.
            val target = targets.first()
            val alternatives = choices.filter { it != target.actualChoice && it in target.legal }
            for (replacement in alternatives) {
                val fork = CultivationReplayFork(target.roundNumber,
                    base.summary.players.first { it.playerId.value == target.playerId }.playerId,
                    target.consultationIndex, replacement)
                // Retain Chronicle only for small representative sample; regenerate baseline with entries for those.
                val preserve = saved < keep
                val original = if (preserve) engine.run(ms, ms + 99173, retainChronicle = true) else base
                val pair = engine.compare(ms, ms + 99173, fork, original, retainChronicle = preserve)
                val before = pair.original.summary.players.single { it.playerId == fork.playerId }
                val after = pair.alternative.summary.players.single { it.playerId == fork.playerId }
                val a = before.ownedDiceSignature; val b = after.ownedDiceSignature
                val caseDir = if (preserve) "cases/${sample.toString().padStart(5,'0')}-${replacement.name}" else ""
                if (preserve) CompostReplayReportWriter.write(pair, out.resolve(caseDir))
                val d = pair.deltaForPlayer
                val cols = listOf(arm, ms, target.playerId, target.roundNumber, target.consultationIndex, replacement,
                    before.totalVp, after.totalVp, d.totalVp, d.battleVp, d.plantVp, d.wispVp, d.wounds,
                    d.diceCount, d.dicePower, b.d4-a.d4, b.d6-a.d6, b.d8-a.d8, b.d10-a.d10,
                    b.d12-a.d12, b.d20-a.d20, d.plants, d.winShare, caseDir)
                Files.writeString(tsv, cols.joinToString("\t") + "\n", java.nio.file.StandardOpenOption.APPEND)
                successful++
                if (preserve) saved++
            }
            if ((sample+1) % 25 == 0) println("$arm samples=${sample+1}/$games eligible=$eligible pairs=$successful")
        }
        Files.writeString(out.resolve("summary.txt"), "arm=$arm\nsamples=$games\nseed=$seed\nnaturalCompostGames=$eligible\npairedAlternatives=$successful\nchroniclesSaved=$saved\n" +
            "Interpret pairs.tsv as alternative-minus-Compost; seeded continuations can diverge in random draw order. Do not claim per-game causality from a single pair.\n")
        println("FINISHED $arm: $eligible games had a natural Compost choice; $successful counterfactual pairs, $saved complete Chronicle pairs; output=$out")
    } finally { app.close() }
}
