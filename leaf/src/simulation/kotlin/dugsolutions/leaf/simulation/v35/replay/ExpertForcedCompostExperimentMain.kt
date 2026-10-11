package dugsolutions.leaf.simulation.v35.replay

import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.cultivation.learnedCultivationFactory
import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.game.replay.ReplayMainAction
import org.koin.dsl.koinApplication
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.APPEND

/** Matched expert-versus-expert study; forced Compost is a deviation, not a different policy. */
fun main(args: Array<String>) {
    val opts = args.map { it.split("=", limit = 2) }.filter { it.size == 2 }
        .associate { it[0].removePrefix("--") to it[1] }
    fun opt(name: String, default: String) = opts[name] ?: default
    val samples = opt("games", "500").toInt().also { require(it in 1..100000) }
    val maxForced = opt("max-forced", "3").toInt().also { require(it in 1..10) }
    val chronicles = opt("chronicles", "12").toInt().also { require(it >= 0) }
    val baseSeed = opt("seed", "860000").toLong()
    val numPlayers = opt("players", "2").toInt().also { require(it in 2..4) }
    val mainPath = Paths.get(requireNotNull(opts["main-weights"]) { "--main-weights=PATH required" })
    val buyPath = Paths.get(requireNotNull(opts["buy-weights"]) { "--buy-weights=PATH required" })
    val roundPath = Paths.get(opt("round-overrides", "data/research/4p/resync/round-resync-current.csv"))
    val output = Paths.get(opt("output", "output/experiments/expert-forced-compost"))
    Files.createDirectories(output)
    val app = koinApplication { modules(appModules) }
    try {
        val k = app.koin
        loadCultivationResearchCards(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(),
            k.get<WispCardRegistry>(), k.get<WispCardManager>(),
            k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
        val plants = k.get<PlantCardManager>()
        val grove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plants.getCard(it)) }
        val roundValues = RoundExperimentResearchConfig.resolve(roundPath,
            k.get<RoundCardManager>().getAllCards().cards).values
        val expert = learnedCultivationFactory(LearnedCultivationMainWeights.load(mainPath),
            LearnedBuyWeights.load(buyPath))
        val engine = CompostCounterfactualRunner(k.get<GameFactory>(), k.get<GameRunner>(),
            grove, List(numPlayers) { expert }, parseRoundSetup("3/2/2"), roundValues)
        val header = "seed\tseat\trequested_forced\tactual_forced\tcontrol_vp\tforced_vp\tdelta_vp\tcontrol_battle_vp\tforced_battle_vp\tdelta_battle_vp\tcontrol_plant_vp\tforced_plant_vp\tdelta_plant_vp\tcontrol_win_share\tforced_win_share\tdelta_win_share\tdelta_dice_count\tdelta_dice_power\tdelta_d4\tdelta_d6\tdelta_d8\tdelta_d10\tdelta_d12\tdelta_d20\tcontrol_compost_selected\tcase_dir"
        val file = output.resolve("pairs.tsv")
        Files.writeString(file, header + "\n")
        var written = 0
        var cases = 0
        var empty = 0
        for (s in 0 until samples) {
            val seed = baseSeed + s
            val strategySeed = seed + 99173
            val seat = s % numPlayers
            val base = engine.run(seed, strategySeed, retainChronicle = false)
            val basePlayer = base.summary.players.single { it.seat == seat }
            val alreadyComposts = base.decisions.count { d -> d.playerId == basePlayer.playerId.value &&
                ((d.actualChoice == ReplayMainAction.ROUND_EFFECT_1 &&
                    d.firstEffect == dugsolutions.leaf.v35.effect.GameEffect.UPGRADE_DIE_FROM_HAND) ||
                    (d.actualChoice == ReplayMainAction.ROUND_EFFECT_2 &&
                    d.secondEffect == dugsolutions.leaf.v35.effect.GameEffect.UPGRADE_DIE_FROM_HAND)) }
            for (n in 1..maxForced) {
                val save = cases < chronicles
                val original = if (save) engine.run(seed, strategySeed, retainChronicle = true) else base
                val changed = engine.run(seed, strategySeed, retainChronicle = save,
                    forceCompostPlayerId = basePlayer.playerId, forceCompostLimit = n)
                if (changed.forcedCompostCount == 0) { empty++; continue }
                val a = original.summary.players.single { it.playerId == basePlayer.playerId }
                val b = changed.summary.players.single { it.playerId == basePlayer.playerId }
                val x = a.ownedDiceSignature; val y = b.ownedDiceSignature
                val directory = if (save) "cases/${s.toString().padStart(5,'0')}-seat$seat-force$n" else ""
                if (save) {
                    val path = output.resolve(directory)
                    Files.createDirectories(path)
                    Files.writeString(path.resolve("control.txt"),
                        "seed=$seed seat=$seat forced=0 vp=${a.totalVp}\n" + original.chronicle.joinToString("\n"))
                    Files.writeString(path.resolve("forced.txt"),
                        "seed=$seed seat=$seat requested=$n applied=${changed.forcedCompostCount} vp=${b.totalVp}\n" + changed.chronicle.joinToString("\n"))
                    Files.writeString(path.resolve("decisions.txt"),
                        "CONTROL\n" + original.decisions.filter { it.playerId == a.playerId.value }.joinToString("\n") +
                            "\nFORCED\n" + changed.decisions.filter { it.playerId == a.playerId.value }.joinToString("\n"))
                    cases++
                }
                val row = listOf(seed,seat,n,changed.forcedCompostCount,a.totalVp,b.totalVp,b.totalVp-a.totalVp,
                    a.battleStrikeVp,b.battleStrikeVp,b.battleStrikeVp-a.battleStrikeVp,
                    a.plantVp,b.plantVp,b.plantVp-a.plantVp,a.winShare,b.winShare,b.winShare-a.winShare,
                    b.finalDiceCount-a.finalDiceCount,b.finalDicePower-a.finalDicePower,
                    y.d4-x.d4,y.d6-x.d6,y.d8-x.d8,y.d10-x.d10,y.d12-x.d12,y.d20-x.d20,
                    alreadyComposts,directory)
                Files.writeString(file, row.joinToString("\t") + "\n", APPEND)
                written++
            }
            if ((s+1)%25==0) println("samples=${s+1}/$samples comparisons=$written no-forcible=$empty")
        }
        Files.writeString(output.resolve("manifest.txt"),
            "EXPERT FORCED ORIGINAL COMPOST\nplayers=$numPlayers\nsamples=$samples\nmaxForced=$maxForced\nseed=$baseSeed\n" +
                "mainWeights=$mainPath\nbuyWeights=$buyPath\nroundOverrides=$roundPath\n" +
                "Comparisons=$written\nNoForced=$empty\n" +
                "Control: identical learned Main and Buy policy in every seat. Treatment forces the first N legal Compost opportunities where expert would choose another action.\n" +
                "Within each seed all branches have the same starting seed, but later random consumption may diverge. Individual VP differences are not isolated causal effects.\n")
        println("Finished: $written paired outcomes; archive-ready directory: $output")
    } finally { app.close() }
}
