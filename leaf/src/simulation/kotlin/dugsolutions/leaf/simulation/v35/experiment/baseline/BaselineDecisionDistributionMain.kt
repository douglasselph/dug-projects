package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import org.koin.dsl.koinApplication
import java.io.File

/** Opt-in, fixed-seed Chronicle diagnostics. No decision-policy changes. */
fun main(args: Array<String>) {
    val values = args.associate {
        require(it.startsWith("--") && '=' in it) { "Expected --key=value: $it" }
        val split = it.removePrefix("--").split('=', limit = 2)
        split[0] to split[1]
    }
    val games = (values["games"] ?: "12").toInt().also { require(it in 1..10000) }
    val players = (values["players"] ?: "4").toInt().also { require(it in 2..4) }
    val baseSeed = (values["base-seed"] ?: "12000").toLong()
    val strategySeed = (values["strategy-seed"] ?: "22000").toLong()
    val output = File(values["output"] ?: "build/reports/human-baseline-5b12a").apply { mkdirs() }
    val koinApp = koinApplication { modules(appModules) }
    try {
        val k = koinApp.koin
        loadCatalogs(k.get<PlantCardRegistry>(), k.get<PlantCardManager>(),
            k.get<WispCardRegistry>(), k.get<WispCardManager>(),
            k.get<RoundCardRegistry>(), k.get<RoundCardManager>())
        val plants = FirstGameDefault.PLANT_NAMES.map { name ->
            requireNotNull(k.get<PlantCardManager>().getCard(name)) { "Unknown Plant: $name" }
        }
        val factory = k.get<GameFactory>()
        val runner = k.get<GameRunner>()
        val labels = sortedMapOf<String, Int>()
        val families = sortedMapOf<String, Int>()
        val marginBuckets = sortedMapOf<String, Int>()
        val reasons = sortedMapOf<String, Int>()
        val buys = sortedMapOf<String, Int>()
        val representative = arrayListOf<String>()
        var recorded = 0
        var scoredAlternatives = 0
        var buyDecisions = 0
        repeat(games) { index ->
            val game = factory(GameConfig(
                selectedPlantCards = plants,
                playerDecisionFactories = List(players) { PlayerDecisionFactory.humanBaseline() },
                roundSetup = GameRoundSetup.standard(),
                seed = baseSeed + index,
                strategySeed = strategySeed + index,
                recordDecisionReasoning = true
            ))
            runner.run(game)
            for (entry in game.chronicle.entries) {
                when (entry) {
                    is GameEntry.DecisionReasoning -> {
                        recorded++
                        labels.bump(entry.choiceLabel)
                        MainActionFamily.classify(entry.choiceLabel)?.let { families.bump(it) }
                        val other = entry.alternatives.filterNot { it.selected }
                        scoredAlternatives += entry.alternatives.size
                        val margin = if (other.isEmpty()) null else entry.total - other.maxOf { it.total }
                        marginBuckets.bump(when {
                            margin == null -> "no-alternative"
                            margin < 0 -> "ERROR-negative"
                            margin == 0 -> "tie-0"
                            margin <= 5 -> "close-1-to-5"
                            margin <= 20 -> "medium-6-to-20"
                            else -> "large-over-20"
                        })
                        entry.adjustments.forEach { reasons.bump(it.reason) }
                        if (representative.size < 120 && entry.alternatives.size > 1) {
                            representative += buildString {
                                appendLine("game=${index + 1} player=${entry.playerId} selected=${entry.choiceLabel} total=${entry.total} margin=${margin ?: "NA"}")
                                entry.alternatives.sortedByDescending { it.total }.take(8).forEach { alt ->
                                    appendLine("  ${if (alt.selected) '*' else '-'} ${alt.choiceLabel}: base=${alt.baseScore} total=${alt.total} " +
                                        alt.adjustments.joinToString("; ") { "${it.amount}: ${it.reason}" })
                                }
                            }
                        }
                    }
                    is GameEntry.BuyDecision -> {
                        buyDecisions++
                        buys.bump("${entry.outcome} / ${entry.selectedKind ?: "none"}")
                    }
                    else -> Unit
                }
            }
        }
        fun File.writeCounts(map: Map<String, Int>) = printWriter().use { writer ->
            writer.println("label\tcount\tpercent")
            val total = map.values.sum().coerceAtLeast(1)
            map.entries.sortedWith(compareByDescending<Map.Entry<String,Int>> { it.value }.thenBy { it.key }).forEach {
                writer.println("${it.key.replace('\t',' ')}\t${it.value}\t${"%.2f".format(java.util.Locale.ROOT, 100.0 * it.value / total)}")
            }
        }
        File(output, "selected-choices.tsv").writeCounts(labels)
        // Percentages here are relative to recognized Main/FinalMain choices only.
        // Subordinate scored decisions never enter this denominator.
        File(output, "action-families.tsv").writeCounts(families)
        File(output, "score-margins.tsv").writeCounts(marginBuckets)
        File(output, "score-adjustments.tsv").writeCounts(reasons)
        File(output, "buy-decisions.tsv").writeCounts(buys)
        File(output, "sample-explanations.txt").writeText(representative.joinToString("\n"))
        File(output, "summary.txt").writeText("""games=$games
players=$players
baseSeed=$baseSeed
strategySeed=$strategySeed
recordedScoredDecisions=$recorded
recordedAlternatives=$scoredAlternatives
buyDecisionEvents=$buyDecisions
note=These are scored Chronicle decisions, not an exhaustive count of every executed Main Action.
recordedMainAndFinalMain=${families.values.sum()}
recognizedMainAndFinalMain=${families.filterKeys { it != "Unrecognized Main" }.values.sum()}
unrecognizedMainAndFinalMain=${families["Unrecognized Main"] ?: 0}
note=action-families.tsv percentages use only scored Main(...) and FinalMain(...) selections as their denominator.
note=Bare first-Battle-Main choice labels are not safely identifiable as Main actions and are excluded; this is not an exhaustive executed-action census.
note=Unrecognized Main labels are included in the denominator and identified explicitly, rather than silently discarded.
note=Buy decisions are Chronicle events and should not be added to the scored-choice denominator.
""")
        println("Human Baseline 5B.12A diagnostics: ${output.absolutePath}")
        println("Scored decisions: $recorded; Buy decision events: $buyDecisions")
        println("Scored Main/FinalMain action categories (not all executed Main actions): $families")
    } finally {
        koinApp.close()
    }
}

private fun MutableMap<String, Int>.bump(key: String) { this[key] = (this[key] ?: 0) + 1 }

