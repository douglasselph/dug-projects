package dugsolutions.leaf.simulation.v35.replay

import dugsolutions.leaf.simulation.v35.experiment.round.RoundExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.learning.cultivation.learnedCultivationFactory
import dugsolutions.leaf.simulation.v35.learning.cultivation.loadCultivationResearchCards
import dugsolutions.leaf.simulation.v35.learning.buy.parseRoundSetup
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.replay.CultivationReplayDecision
import dugsolutions.leaf.v35.game.replay.ReplayMainAction
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionExperimentConfiguration
import dugsolutions.leaf.v35.player.decision.baseline.scoring.HumanScoringConfiguration
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
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
import java.nio.file.StandardOpenOption.APPEND
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoreValue
import java.util.Properties

/** Within-rule expert-vs-expert comparisons, switching only one seat to scored Human when Compost is legal. */
fun main(args: Array<String>) {
    val options = args.map { it.split("=", limit=2) }.filter { it.size==2 }
        .associate { it[0].removePrefix("--") to it[1] }
    fun option(key:String, fallback:String) = options[key] ?: fallback
    val games = option("games", "100").toInt().also { require(it in 1..100000) }
    val chronicles = option("chronicles", "12").toInt().also { require(it >= 0) }
    val seedStart = option("seed", "950000").toLong()
    val weights = option("weights", "-20,0,10,20,40,60").split(',').map { it.trim().toInt() }.distinct()
    val out = Paths.get(option("output", "output/experiments/compost-weight-sweep"))
    val scoringPath = Paths.get(option("human-scoring", "data/ai/human-scoring.properties"))
    val baseRounds = Paths.get(option("round-base", "data/research/resync/round-resync-current.csv"))
    val variant = option("variant", "original").lowercase()
    require(variant in setOf("original", "keep-no-rewards"))
    val mainWeights = Paths.get(option("main-weights", "data/ai/2p/cultivation-main-original-compost.weights"))
    val buyWeights = Paths.get(option("buy-weights", "data/ai/2p/buy-original-compost.weights"))
    val effectiveCsv = Paths.get(requireNotNull(options["round-overrides"]) { "--round-overrides=PATH required (launcher generates this)" })
    val humanScoring = HumanScoringConfiguration.load(scoringPath)
    Files.createDirectories(out)
    val app = koinApplication { modules(appModules) }
    try {
        val k = app.koin
        loadCultivationResearchCards(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(),
            k.get<WispCardRegistry>(), k.get<WispCardManager>(), k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
        val grove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(k.get<PlantCardManager>().getCard(it)) }
        val roundValues = RoundExperimentResearchConfig.resolve(effectiveCsv,
            k.get<RoundCardManager>().getAllCards().cards).values
        val expert = learnedCultivationFactory(LearnedCultivationMainWeights.load(mainWeights), LearnedBuyWeights.load(buyWeights))
        val factories = List(2) { expert }
        val gameFactory = k.get<GameFactory>()
        val gameRunner = k.get<GameRunner>()
        val compostBase = (humanScoring.value("compost.base", ScoreValue.Fixed(75)) as? ScoreValue.Fixed)?.amount
            ?: error("compost.base must be a fixed integer for the historical sweep")
        fun engine(seat: Int?, weight: Int): CompostCounterfactualRunner {
            val experiment = if (seat == null) null else Properties().apply {
                setProperty("cultivation.main.policy", "HUMAN_WHEN_COMPOST_AVAILABLE")
            }.let(DecisionExperimentConfiguration::fromProperties)
            return CompostCounterfactualRunner(gameFactory, gameRunner, grove, factories,
                parseRoundSetup("3/2/2"), roundValues, eventBasedDieRolls=true,
                sharedPercentileDieRolls=true, decisionExperiment=experiment,
                humanScoring=if (seat == null) humanScoring else humanScoring.withValue("compost.base", ScoreValue.Fixed(compostBase + weight)),
                decisionExperimentSeats=seat?.let { setOf(it) })
        }
        val reference = engine(null, 0)
        val outputFile = out.resolve("pairs.tsv")
        Files.writeString(outputFile,
            "variant\tweight\tseed\tseat\tcontrol_vp\thybrid_vp\tdelta_vp\tcontrol_margin\thybrid_margin\tdelta_margin\tcontrol_win_share\thybrid_win_share\tdelta_win_share\tcontrol_battle_vp\thybrid_battle_vp\tcontrol_plant_vp\thybrid_plant_vp\tcontrol_dice_power\thybrid_dice_power\tcontrol_compost_opportunities\thybrid_compost_opportunities\tcontrol_compost_uses\thybrid_compost_uses\tfirst_policy_difference\tmatched_rolls\tcross_size_rolls\troll_mismatches\tcontrol_only_rolls\thybrid_only_rolls\tcase_dir\n")
        var saved = 0
        val baseline = HashMap<Long, RecordedReplayGame>()
        for (i in 0 until games) {
            val mechanicalSeed = seedStart+i
            val strategySeed = mechanicalSeed+99173
            val seat = i%2
            val control = baseline.getOrPut(mechanicalSeed) { reference.run(mechanicalSeed, strategySeed, retainChronicle=false) }
            for (weight in weights) {
                val hybrid = engine(seat, weight).run(mechanicalSeed, strategySeed, retainChronicle=false)
                val before = control.summary.players.single { it.seat==seat }
                val after = hybrid.summary.players.single { it.seat==seat }
                val beforeOpp = control.summary.players.single { it.seat!=seat }
                val afterOpp = hybrid.summary.players.single { it.seat!=seat }
                val differences = ReplayEquivalenceDiagnostics.compareRolls(control.rollEvents,hybrid.rollEvents)
                check(differences.unexpectedMismatches == 0) { "Event mismatch seed=$mechanicalSeed weight=$weight: $differences" }
                val baselineActions = compostStats(control.decisions, before.playerId.value)
                val hybridActions = compostStats(hybrid.decisions, after.playerId.value)
                val initialChoices = control.decisions.filter { it.playerId==before.playerId.value }
                val laterChoices = hybrid.decisions.filter { it.playerId==after.playerId.value }
                val firstDifference = initialChoices.zip(laterChoices).firstOrNull { (a,b) ->
                    a.roundNumber!=b.roundNumber || a.consultationIndex!=b.consultationIndex || a.actualChoice!=b.actualChoice
                }?.let { (a,b) -> "round=${a.roundNumber} consultation=${a.consultationIndex} ${a.actualChoice}->${b.actualChoice}" }.orEmpty()
                val directory = if (saved < chronicles && firstDifference.isNotEmpty()) {
                    "cases/${variant}-${i.toString().padStart(5,'0')}-seat$seat-weight$weight"
                } else ""
                if (directory.isNotEmpty()) {
                    val path = out.resolve(directory); Files.createDirectories(path)
                    val fullControl = reference.run(mechanicalSeed,strategySeed,retainChronicle=true)
                    val fullHybrid = engine(seat,weight).run(mechanicalSeed,strategySeed,retainChronicle=true)
                    Files.writeString(path.resolve("control.txt"),fullControl.chronicle.joinToString("\n"))
                    Files.writeString(path.resolve("hybrid.txt"),fullHybrid.chronicle.joinToString("\n"))
                    Files.writeString(path.resolve("control-decisions.txt"),initialChoices.joinToString("\n"))
                    Files.writeString(path.resolve("hybrid-decisions.txt"),laterChoices.joinToString("\n"))
                    saved++
                }
                val controlMargin = before.totalVp-beforeOpp.totalVp
                val hybridMargin = after.totalVp-afterOpp.totalVp
                val row = listOf(variant,weight,mechanicalSeed,seat,before.totalVp,after.totalVp,after.totalVp-before.totalVp,
                    controlMargin,hybridMargin,hybridMargin-controlMargin,before.winShare,after.winShare,after.winShare-before.winShare,
                    before.battleStrikeVp,after.battleStrikeVp,before.plantVp,after.plantVp,before.finalDicePower,after.finalDicePower,
                    baselineActions.first,hybridActions.first,baselineActions.second,hybridActions.second,firstDifference,
                    differences.matchedSameSize+differences.matchedCrossSize,differences.matchedCrossSize,
                    differences.unexpectedMismatches,differences.controlOnly,differences.comparisonOnly,directory)
                Files.writeString(outputFile,row.joinToString("\t")+"\n",APPEND)
            }
            if ((i+1)%25==0) println("$variant: ${i+1}/$games seeds, ${weights.size} weights each")
        }
        Files.writeString(out.resolve("manifest.txt"),
            "variant=$variant\ngames=$games\nseed=$seedStart\nweights=${weights.joinToString(",")}\n"+
            "mainWeights=$mainWeights\nbuyWeights=$buyWeights\nroundBase=$baseRounds\nroundOverrides=$effectiveCsv\n"+
            "humanScoring=$scoringPath\neventBasedDieRolls=true\nsharedPercentileDieRolls=true\n"+
            "treatment=one rotating seat uses HUMAN_WHEN_COMPOST_AVAILABLE with compost.base adjustment; opponent is always learned\n"+
            "control=two identical learned policies; same seeds and configuration for matched rule arm\n"+
            "NOTE: trained weights MUST match this variant for valid expert inference; initial baseline scores are provisional.\n")
        println("Completed $variant: ${games*weights.size} comparisons, results=$out")
    } finally { app.close() }
}

private fun compostStats(decisions: List<CultivationReplayDecision>, playerId: Int): Pair<Int,Int> {
    fun compost(effect: GameEffect) = effect in setOf(GameEffect.UPGRADE_DIE_FROM_HAND,
        GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS,GameEffect.UPGRADE_DIE_AND_USE_NOW)
    val legal = decisions.filter { d -> d.playerId==playerId &&
        ((ReplayMainAction.ROUND_EFFECT_1 in d.legal && compost(d.firstEffect)) ||
            (ReplayMainAction.ROUND_EFFECT_2 in d.legal && compost(d.secondEffect))) }
    return legal.size to legal.count { d ->
        (d.actualChoice==ReplayMainAction.ROUND_EFFECT_1 && compost(d.firstEffect)) ||
            (d.actualChoice==ReplayMainAction.ROUND_EFFECT_2 && compost(d.secondEffect))
    }
}
